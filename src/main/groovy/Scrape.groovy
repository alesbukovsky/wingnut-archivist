import geb.Browser

cfg = [
    'rootUrl'    : 'http://www.wingnutwings.com/ww/',
    'outputFile' : args[0]
]

def output = new File(cfg.outputFile)

Browser.drive {
    println "Scrape: ${cfg.rootUrl}"
    go cfg.rootUrl

    def ks = $('li.modelkitsets').$('ul').$('li').$('ul').$('a')*.attr('href')
    def ds = $('li.decals').$('ul').$('li').$('ul').$('a')*.attr('href')

    println "Found: ${ks.size() + ds.size()} products (${ks.size()} kits, ${ds.size()} decals)"

    ks.plus(ds).eachWithIndex { p, x ->
        println "Product [${x + 1}]: ${p}"
        go p
        def l = $('a', text:'Colour schemes')
        if (l) {
            l.click()
            def is = $('a', class: 'lightboxlink')
            println "  - ${is.size()} color schemes"
            is.each { i ->
                println "  - ${i.attr('href')}"
                output << '"' + i.attr('href') + '"\n'
            }
        } else {
            println '  - no color schemes page'
        }
    }
}
