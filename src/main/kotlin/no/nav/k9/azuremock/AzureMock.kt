package no.nav.k9.azuremock

import io.ktor.application.*
import io.ktor.features.origin
import io.ktor.html.respondHtml
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.getOrFail
import kotlinx.html.*
import no.nav.helse.dusseldorf.ktor.core.getOptionalString
import no.nav.helse.dusseldorf.testsupport.http.AzureToken
import no.nav.helse.dusseldorf.testsupport.http.AzureWellKnown
import no.nav.helse.dusseldorf.testsupport.http.TokenRequest
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import org.slf4j.LoggerFactory
import java.net.URLDecoder

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private object Konstanter {
    internal const val basePath = "/v2.0"
    internal const val wellKnownPath = "$basePath/.well-known/openid-configuration"
    internal const val tokenPath = "$basePath/token"
    internal const val authorizationPath = "$basePath/authorize"
    internal const val jwksPath = "$basePath/jwks"
}

private val logger = LoggerFactory.getLogger("no.nav.AzureMock")

@KtorExperimentalAPI
fun Application.AzureMock() {
    val issuerHost = environment.config.getOptionalString("no.nav.issuer_host", false)
    fun ApplicationRequest.issuer() = when (issuerHost) {
        null -> baseUrl()
        else -> baseUrl(host = issuerHost)
    }.plus(Konstanter.basePath)

    install(Routing) {
        get(Konstanter.wellKnownPath) {
            logger.info("${call.request.httpMethod.value}@${call.request.uri}")
            val baseUrl = call.request.baseUrl()
            val wellKnownResponse = AzureWellKnown.response(
                    issuer = call.request.issuer(),
                    tokenEndpoint = "$baseUrl${Konstanter.tokenPath}",
                    authorizationEndpoint = call.request.authorizationEndpoint(),
                    jwksUri = "$baseUrl${Konstanter.jwksPath}"
            )
            call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = wellKnownResponse
            )
        }

        get(Konstanter.jwksPath) {
            logger.info("${call.request.httpMethod.value}@${call.request.uri}")
            call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = Azure.V2_0.getPublicJwk()
            )
        }

        post(Konstanter.tokenPath) {
            logger.info("${call.request.httpMethod.value}@${call.request.uri}")
            val tokenResponse = AzureToken.response(
                    request = KtorTokenRequest(
                            call = call,
                            body = call.receiveText()
                    ),
                    issuer = call.request.issuer()
            )
            call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = tokenResponse
            )
        }

        get(Konstanter.authorizationPath) {
            logger.info("${call.request.httpMethod.value}@${call.request.uri}")
            val clientId = call.request.queryParameters.getOrFail("client_id")
            val redirectUri = call.request.queryParameters.getOrFail("redirect_uri")
            val scope = call.request.queryParameters.getOrFail("scope")
            val state = call.request.queryParameters.getOrFail("state")
            val nonce = call.request.queryParameters.getOrFail("nonce")

            // Optional
            val sattUserId = call.request.queryParameters["user_id"]?:""
            val sattName = call.request.queryParameters["name"]?:""
            val autoSubmit = !sattUserId.isBlank() && !sattName.isBlank()

            call.respondHtml {
                head {
                    title { +"Azure Mock Login Input" }
                }
                body {
                    if (autoSubmit) { onLoad = "document.form.submit()" }
                    form(action = Konstanter.authorizationPath,
                         encType = FormEncType.applicationXWwwFormUrlEncoded,
                         method = FormMethod.post) {
                        name = "form"
                        acceptCharset = "utf-8"
                        h1 { +"Azure Mock Login Input"}
                        p {
                            label { +"Bruker ID: " }
                            textInput(name = "user_id") { value = sattUserId }
                        }
                        p {
                            label { +"Navn: " }
                            textInput(name = "name") { value = sattName }
                        }
                        hiddenInput(name = "client_id") { value = clientId }
                        hiddenInput(name = "scope") {  value = scope }
                        hiddenInput(name = "state") { value = state }
                        hiddenInput(name = "nonce") { value = nonce }
                        hiddenInput(name = "redirect_uri") { value = redirectUri }
                        submitInput{ value = "Fortsett"}
                    }
                }
            }
        }

        post(Konstanter.authorizationPath) {
            logger.info("${call.request.httpMethod.value}@${call.request.uri}")
            val parameters = call.receiveParameters()
            val userId = parameters.getOrFail("user_id")
            val sattName = parameters["name"] ?: userId
            val scope = parameters.getOrFail("scope")
            val state = parameters.getOrFail("state")
            val nonce = parameters.getOrFail("nonce")
            val redirectUri = parameters.getOrFail("redirect_uri")

            val code = AzureToken.Code(
                    userId = userId,
                    name = sattName,
                    scopes = scope,
                    nonce = nonce
            )

            call.respondHtml {
                body {
                    onLoad = "document.form.submit()"
                    form(action = redirectUri,
                         encType = FormEncType.applicationXWwwFormUrlEncoded,
                         method = FormMethod.post) {
                        name = "form"
                        acceptCharset = "utf-8"
                        hiddenInput(name = "code") { value = code.toString() }
                        hiddenInput(name = "state") { value = state }
                    }
                }
            }
        }
    }
}

private class KtorTokenRequest(
        call: ApplicationCall,
        body: String
): TokenRequest {
    private val urlDecodedBody = URLDecoder.decode(body, Charsets.UTF_8)
    private val authorizationHeader = call.request.header(HttpHeaders.Authorization)
    override fun urlDecodedBody(): String = urlDecodedBody
    override fun authorizationHeader() = authorizationHeader
}

private fun ApplicationRequest.baseUrl(host: String = host()) = "${origin.scheme}://$host:${port()}"
private fun ApplicationRequest.baseUrlAuthorizationEndpoint() = baseUrl(host = "localhost")
private fun ApplicationRequest.authorizationEndpoint() : String {
    return if (call.request.queryParameters.isEmpty()) {
        "${call.request.baseUrlAuthorizationEndpoint()}${Konstanter.authorizationPath}"
    } else {
        "${call.request.baseUrlAuthorizationEndpoint()}${Konstanter.authorizationPath}?${call.request.queryString()}"
    }
}