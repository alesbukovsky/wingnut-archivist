import geb.Browser

cfg = [
    'rootUrl'      : 'http://www.wingnutwings.com/ww/',
    'schemes'      : args[0],
    'instructions' : args[1]
]

def schemes = new File(cfg.schemes)
def instructions = new File(cfg.instructions)

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
            println "  - ${is.size()} color scheme(s)"
            is.each { i ->
                println "  - ${i.attr('href')}"
                schemes << '"' + i.attr('href') + '"\n'
            }
        } else {
            println '  - no color schemes page'
        }

        l =  $('a', text:'Instructions')
        if (l) {
            l.click()
            def is = $('a', class: 'lightboxlink')
            println "  - ${is.size()} instruction sheet(s)"
            is.each { i ->
                println "  - ${i.attr('href')}"
                instructions << '"' + i.attr('href') + '"\n'
            }

            is = $('a', class: 'lightboxpdflink')
            println "  - ${is.size()} instruction pdf file(s)"
            is.each { i ->
                println "  - ${i.attr('href')}"
                instructions << '"' + i.attr('href') + '"\n'
            }
        } else {
            println '  - no instructions page'
        }

        l =  $('a', text:'Hints & Tips')
        if (l) {
            l.click()
            def is = $('a', class: 'lightboxlink')
            println "  - ${is.size()} hint sheet(s)"
            is.each { i ->
                println "  - ${i.attr('href')}"
                instructions << '"' + i.attr('href') + '"\n'
            }
        } else {
            println '  - no hints page'
        }
    }
}
