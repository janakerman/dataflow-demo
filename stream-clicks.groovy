import groovy.json.JsonOutput

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadLocalRandom

def cli = new CliBuilder(usage: 'showdate.groovy -[hu]')

cli.with {
    h longOpt: 'help', 'Show usage information'
    u longOpt: 'url', args: 1, argName: 'url', 'URL to post click data to'
}

def options = cli.parse(args)
if (!options) return

if (options.h) {
    cli.usage()
    return
}

final String urlString = options.u
final NUM_USERS = 100
final NUM_THREADS = 20

def postClick = { String clickData ->
    try {
        final URL url = new URL(urlString)
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection()
        connection.setRequestMethod("POST")
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.doOutput = true
        connection.doInput = true

        def writer = new OutputStreamWriter(connection.outputStream)
        writer.write(clickData)
        writer.flush()
        writer.close()
        connection.connect()
    } catch (Exception e) {
        println "Error posting action"
        println e
    }
}

def keepPostingMockData = {
    while (true) {
        int id = ThreadLocalRandom.current().nextInt(0, NUM_USERS)
        String action = ['page-view', 'click'][ThreadLocalRandom.current().nextInt(0, 2)]

        final jsonString = JsonOutput.toJson([userId: id, action: action])
        println "Posting json data: $jsonString"
        postClick(jsonString)
    }
}

ExecutorService executorService = Executors.newFixedThreadPool(NUM_THREADS)
(1..NUM_THREADS).each {
    CompletableFuture.runAsync({ keepPostingMockData() }, executorService)
}
