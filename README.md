## Wingnut Archivist

A set of _ad-hoc_ tools for archiving images from [Wingnut Wings](http://www.wingnutwings.com/) website after it closed down in April 2020. The intention is to preserve this valuable resource for WWI aircraft enthusiasts, not to infringe on any potential copyright.

The archive (4652 images) is currently available [here](https://photos.app.goo.gl/PkGkLX5gRzKTrzrB7).

Note that the following is not a read-to-go software kit, there is still a good deal of manual tweaking needed.

#### Observations

* The content of the entire website is dynamically built via a JavaScript framework. This makes static HTML scraping impossible.
* Image name is essentially a short description.
* Image thumbnails have the same name as originals, only with `thumb_` prefix.
* All photos linked to individual products (kits or decals) is also published in the gallery.

#### Scraping Gallery Photos

Used Firefox developer tools to capture thumbnail images requests as manually clicked thru gallery pages. Filtered those by `thumb_` prefix and exported in [HAR](https://en.wikipedia.org/wiki/HAR_(file_format)) format.

Used [jq](https://stedolan.github.io/jq/) tool to extract image URLs from HAR file:
```
jq ".log.entries[] | .request.url" ./data/archive.har > ./data/archive-urls.txt
```

Removed `thumb_` prefixes from all URLs.

Downloaded original images. This effectively overrides possible duplicates with the same file name:
```
xargs -n 1 curl -O < ./data/archive-urls.txt
```

#### Scraping Color Schemes

Color schemes are only available on the individual product pages, i.e. kits and decals.

Used [geb](https://gebish.org/) tool and a headless browser to traverse the dynamically modified HTML.
```
gradle scrape
```

#### Uploading

Used Google [Photos API](https://developers.google.com/photos/library/guides/overview) to upload all images into a dedicated album.
```
gradle upload
```
  
The original file names are partially reformatted for readability, e.g. HTML `%20` entity is replaced with a space character.

The image description is derived from the file name with additional formatting, e.g. `~` is replaced with `/`.


#### Analysis

Post-upload analysis reveals that Google Photos apparently applies a degree of duplicate image detection. 
```
gradle analyze
```

There are however discrepancies. Total of 4701 images were uploaded. The API rejected 1 of them right away with "already exists" error. Nevertheless the album ended up with 4657 images. There are instances of the same picture under similar file names. The API detected 48 of such cases, bringing the expected count to 4652. Further comparison of the downloaded archive revealed 5 images were actually uploaded twice for an unknown reason. Removing these manually settles the count.

It should be noted however that there are still images in the album that look identical. The corresponding file name are rather different though. It unclear how Google Photos determines the duplicate.


