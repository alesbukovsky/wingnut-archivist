def traces = []
new File(args[0]).newInputStream().eachLine { traces << it }

def dups = traces.countBy{ it }.grep{ it.value > 1 }
def uniques = traces.unique(false);

println "Traces: ${traces.size()}"
println "Duplicates: ${dups.size()}"
println "Uniques: ${uniques.size()} :: ${traces.size() - dups.size()}"

dups.each { println "${it.value} : ${it.key}" }

def downs = []
new File(args[1]).newInputStream().eachLine { downs << it }

println "Downloaded: ${downs.size()}"

def i = 0
downs.each{
    if (!uniques.contains(it)) { println "${++i} - $it" }
}