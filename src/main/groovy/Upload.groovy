import com.google.api.client.auth.oauth2.*
import com.google.api.client.extensions.java6.auth.oauth2.*
import com.google.api.client.extensions.jetty.auth.oauth2.*
import com.google.api.client.googleapis.auth.oauth2.*
import com.google.api.client.http.javanet.*
import com.google.api.client.json.*
import com.google.api.client.json.jackson2.*
import com.google.api.client.util.store.*
import com.google.api.gax.core.*
import com.google.auth.*
import com.google.auth.oauth2.*
import com.google.photos.library.v1.*
import com.google.photos.library.v1.proto.*
import com.google.photos.library.v1.upload.*
import com.google.photos.library.v1.util.*
import com.google.rpc.*
import groovy.io.*

cfg = [
    'albumName'  : 'Wingnut Wings ' + args[0],
    'sourceDir'  : args[1],
    'apiSecret'  : args[2],
    'batchSize'  : 35
]

def upload(PhotosLibraryClient c, a, List<File> l) {
    println "Uploading ${l.size()} files"

    Map<String, String> trail = [:]
    List<NewMediaItem> items = []
    RandomAccessFile file

    l.each { f ->
        println "Uploading: ${f.getName()}"
        file = new RandomAccessFile(f.getCanonicalPath(), "r")
        file.withCloseable {
            UploadMediaItemRequest req = UploadMediaItemRequest.newBuilder()
                .setMimeType("image/jpg")
                .setDataFile(file)
                .build();
            UploadMediaItemResponse res = c.uploadMediaItem(req)

            if (!res.getError().isPresent()) {
                trail[res.getUploadToken().get()] = f.getCanonicalPath()

                def desc = f.getName()
                desc = desc.replace('~', '/')
                desc = desc.replace('.jpg', '')

                items << NewMediaItemFactory.createNewMediaItem(res.getUploadToken().get(), f.getName(), desc)

            } else {
                println "ERROR: ${f.getCanonicalPath()}"
            }
        }
    }

    println "Adding ${items.size()} items to library"

    List<String> ids = []
    BatchCreateMediaItemsResponse res = c.batchCreateMediaItems(items);

    res.getNewMediaItemResultsList().each { r ->
        if (r.getStatus().getCode() == Code.OK_VALUE) {
            ids << r.getMediaItem().id
        } else if (r.getStatus().getCode() == Code.ALREADY_EXISTS_VALUE) {
            println "DUPLICATE: ${trail[r.getUploadToken()]}"
        } else {
            println "ERROR: ${trail[r.getUploadToken()]}"
        }
    }

    println "Adding ${ids.size()} items to album"
    c.batchAddMediaItemsToAlbum(a, ids);
}

println "Authenticating"

JsonFactory json = new JacksonFactory()
GoogleClientSecrets secret = GoogleClientSecrets.load(json, new FileReader(cfg.apiSecret))
DataStore<StoredCredential> store = new FileDataStoreFactory(new File('./build')).getDataStore('credentials');

List<String> scopes = [
    'https://www.googleapis.com/auth/photoslibrary.appendonly'
]

GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(), json, secret, scopes)
    .setCredentialDataStore(store)
    .build();

Credential auth = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");

Credentials creds = UserCredentials.newBuilder()
    .setClientId(secret.getDetails().getClientId())
    .setClientSecret(secret.getDetails().getClientSecret())
    .setRefreshToken(auth.getRefreshToken())
    .build()

PhotosLibrarySettings settings = PhotosLibrarySettings.newBuilder()
    .setCredentialsProvider(FixedCredentialsProvider.create(creds))
    .build();

println "Setting up API client"

PhotosLibraryClient client = PhotosLibraryClient.initialize(settings)
client.withCloseable {

    def album = client.createAlbum(cfg.albumName)
    println "Album created: ${album.title} [${album.id}]"

    def batch = []
    def source = new File(cfg.sourceDir)

    println "Traversing source directory: ${source.getCanonicalPath()}"
    
    source.traverse(type: FileType.FILES, nameFilter: ~/(?i).*\.jpe?g/, maxDepth: -1, sort: { a, b -> a.name <=> b.name } ) { f ->
        batch << f
        if (batch.size() >= cfg.batchSize) {
            upload(client, album.id, batch)
            batch.clear()
        }
    }
    if (!batch.empty) {
        upload(client, album.id, batch)
        batch.clear()
    }
}
