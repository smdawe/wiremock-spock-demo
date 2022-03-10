package io.github.smdawe

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.Fault
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import static com.github.tomakehurst.wiremock.client.WireMock.*

class WiremockExamples extends Specification {

  @Shared
  WireMockServer mockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort().notifier(new Slf4jNotifier(true)))

  @Shared
  String baseUrl

  HttpClient client

  String testUrl
  String testPath

  void setupSpec() {
    mockServer.start()
    baseUrl = "http://localhost:${mockServer.port()}"
  }

  void cleanupSpec() {
    mockServer.stop()
  }

  void setup() {
    client = HttpClient.newHttpClient()
    testPath = "/test/${UUID.randomUUID()}"
    testUrl = "$baseUrl$testPath"
  }

  void 'http get to a JSON api'() {
    given:
    Map<String, String> responseBody = [hello: 'world']
    mockServer.stubFor(get(testPath).willReturn(okJson(JsonOutput.toJson(responseBody))))

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(testUrl))
            .GET()
            .build()
    when:
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

    then:
    response.statusCode() == 200
    response.headers().firstValue('Content-Type').get() == 'application/json'
    new JsonSlurper().parseText(response.body()) == responseBody
  }

  void 'http post to a JSON api'() {
    given:
    Map<String, String> postBody = [hello: 'world']
    mockServer.stubFor(post(testPath)
            .withHeader('Content-Type', equalTo('application/json'))
            .withRequestBody(equalTo(JsonOutput.toJson(postBody)))
            .willReturn(created())
    )

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(testUrl))
            .POST(HttpRequest.BodyPublishers.ofString(JsonOutput.toJson(postBody)))
            .header('Content-Type', 'application/json')
            .build()
    when:
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

    then:
    response.statusCode() == 201
  }

  void 'http get to a JSON api - simulate fault'() {
    given:
    mockServer.stubFor(get(testPath).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)))

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(testUrl))
            .GET()
            .build()
    when:
    client.send(request, HttpResponse.BodyHandlers.ofString())

    then:
    Exception e = thrown(IOException)
    e.message == 'Connection reset by peer'
  }

  void 'http get to a JSON api - timeout'() {
    given:
    int delay = 1_000
    Duration timeout = Duration.ofMillis(500)
    Map<String, String> responseBody = [hello: 'world']
    mockServer.stubFor(get(testPath).willReturn(okJson(JsonOutput.toJson(responseBody)).withFixedDelay(delay)))

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(testUrl))
            .GET()
            .timeout(timeout)
            .build()
    when:
    client.send(request, HttpResponse.BodyHandlers.ofString())

    then:
    Exception e = thrown(IOException)
    e.message == 'request timed out'
  }

  void 'http get to a JSON api - delay'() {
    given:
    int delay = 500
    Duration timeout = Duration.ofMillis(1000)
    Map<String, String> responseBody = [hello: 'world']
    mockServer.stubFor(get(testPath).willReturn(okJson(JsonOutput.toJson(responseBody)).withFixedDelay(delay)))

    HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(testUrl))
            .GET()
            .timeout(timeout)
            .build()
    when:
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

    then:
    response.statusCode() == 200
    response.headers().firstValue('Content-Type').get() == 'application/json'
    new JsonSlurper().parseText(response.body()) == responseBody
  }

}
