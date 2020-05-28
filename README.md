## Wingnut Archivist

A set of _ad-hoc_ tools for archiving images from [Wingnut Wings](http://www.wingnutwings.com/) website after the company abruptly closed down in April 2020. The intention is to preserve this valuable resource for WWI aircraft enthusiasts, not to infringe on any potential copyright.

The archive is currently available thru the following links:

* [Photos](https://photos.app.goo.gl/tz5UqyQuMSqadqwd9) (4096 images)
* [Photos WW2](https://photos.app.goo.gl/z87r239Rp1X3ZxET9) (89 images)
* [Color Schemes](https://photos.app.goo.gl/dnVYpQkzPd12HYY68) (445 images)
* [Instructions](https://photos.app.goo.gl/8U5ECGuXA2YVuzgu7) (2316 images)
* [Instruction booklets](https://drive.google.com/drive/folders/16cwiJkI8oHhw9HQiJQfYoKfqOCYi8Szv?usp=sharing) (90 PDF files)

Note that this is not a read-to-go software kit. There is still a good deal of manual tweaking needed.

#### Observations

* The content of the entire website is dynamically built via a JavaScript framework. This makes static HTML scraping impossible.
* Image name is essentially a short description.
* Image thumbnails have the same name as originals, only with `thumb_` prefix.
* All photos linked to individual products (kits or decals) is also published in the gallery.
* Google Photo API appears to detect, although not always, identical images under different file names.

#### Basic Tools

File download based on the list of URLs. This effectively overrides possible duplicates with the same file name:
```
xargs -n 1 curl -O < ../../data/archive-urls.txt
```

Reformatting file names using regular expressions with [`rename`](https://formulae.brew.sh/formula/rename):
```
rename 's/%20/ /g' *
```

If the number of files is too large (resulting in "_argument list too long_" error), the renaming needs to be piped with `find` command:
```
find . -exec rename 's/%20/ /g' {} +
```

#### Scraping Galleries

Used Firefox developer tools to capture thumbnail images requests as manually clicked thru gallery pages. Filtered those by `thumb_` prefix and exported in [HAR](https://en.wikipedia.org/wiki/HAR_(file_format)) format.

Used [jq](https://stedolan.github.io/jq/) tool to extract image URLs from HAR file:
```
jq ".log.entries[] | .request.url" ./data/archive.har > ./data/archive-urls.txt
```

Removed `thumb_` prefixes from all URLs.

#### Scraping Products

Color schemes, instructions and hint sheets are only available on the individual product pages, i.e. kits and decals.

Used [geb](https://gebish.org/) tool and a headless browser to traverse the dynamically modified HTML and obtain the URLs.
```
gradle scrape
```

Few remaining images were downloaded manually from the general Hints & Tips section.

#### Uploading

Used Google [Photos API](https://developers.google.com/photos/library/guides/overview) to upload all images into a dedicated album.
```
gradle upload
```
  
Instruction booklets (PDF) were uploaded manually to a shared folder in Google Drive.
