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
    'albumName'  : 'Wingnut Wings Archive',
    'batchSize'  : 50,
    'tempDir'    : args[0],
    'apiSecret'  : args[1],
    'rootDir'    : args[2],
    'appendLogs' : Boolean.parseBoolean(args[3]),
    'albumId'    : args[4].toLowerCase() == 'create' ? null : args[4]
]

trace = new PrintWriter(new FileWriter(cfg.tempDir + '/traces', cfg.appendLogs))
error = new PrintWriter(new FileWriter(cfg.tempDir + '/errors', cfg.appendLogs))

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

                def name = f.getName()
                name = name.replace('%20', ' ')
                name = name.replace('+', '-')
                name = name.replace('. ', ' - ')
                name = name.replace('  ', ' ')
                name = name.replace('..', '.')
                name = name.replace('.jpg.jpg', '.jpg')
                name = name.replace('.JPG', '.jpg')

                def desc = name
                desc = desc.replace('~', '/')
                desc = desc.replace('.jpg', '')

                items << NewMediaItemFactory.createNewMediaItem(res.getUploadToken().get(), name, desc)

            } else {
                error.println "ERR: ${f.getCanonicalPath()}"
            }
        }
    }

    println "Adding ${items.size()} items to library"

    List<String> ids = []
    BatchCreateMediaItemsResponse res = c.batchCreateMediaItems(items);

    res.getNewMediaItemResultsList().each { r ->
        if (r.getStatus().getCode() == Code.OK_VALUE) {
            ids << r.getMediaItem().id
            trace.println "${r.getMediaItem().getFilename()}"
        } else if (r.getStatus().getCode() == Code.ALREADY_EXISTS_VALUE) {
            error.println "DUP: ${trail[r.getUploadToken()]}"
        } else {
            error.println "ERR: ${trail[r.getUploadToken()]}"
        }
    }

    println "Adding ${ids.size()} items to album"
    c.batchAddMediaItemsToAlbum(a, ids);
}

println "Authenticating"

JsonFactory json = new JacksonFactory()
GoogleClientSecrets secret = GoogleClientSecrets.load(json, new FileReader(cfg.apiSecret))
DataStore<StoredCredential> store = new FileDataStoreFactory(new File(cfg.tempDir)).getDataStore('credentials');

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

    def album = cfg.albumId
    if (!album) {
        album = client.createAlbum(cfg.albumName).id
        println "Album created: $album"
    } else {
        println "Using existing album: $album"
    }

    def batch = []
    def root = new File(cfg.rootDir)

    println "Traversing root: ${root.getCanonicalPath()}"

    root.traverse(type: FileType.FILES, nameFilter: ~/(?i).*\.jpe?g/, maxDepth: -1) { f ->
        batch << f
        if (batch.size() >= cfg.batchSize) {
            upload(client, album, batch)
            batch.clear()
        }
    }
    if (!batch.empty) {
        upload(client, album, batch)
        batch.clear()
    }
}

error.close()
trace.close()