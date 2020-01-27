package no.nav.k9.azuremock

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
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
import no.nav.helse.dusseldorf.testsupport.http.AzureToken
import no.nav.helse.dusseldorf.testsupport.http.AzureWellKnown
import no.nav.helse.dusseldorf.testsupport.http.TokenRequest
import no.nav.helse.dusseldorf.testsupport.jws.Azure
import java.net.URLDecoder

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private object Konstanter {
    internal const val basePath = "/v2.0"
    internal const val wellKnownPath = "$basePath/.well-known/openid-configuration"
    internal const val tokenPath = "$basePath/token"
    internal const val authorizationPath = "$basePath/authorize"
    internal const val jwksPath = "$basePath/jwks"
}

@KtorExperimentalAPI
fun Application.AzureMock() {
    install(Routing) {
        get(Konstanter.wellKnownPath) {
            val baseUrl = call.request.baseUrl()
            val wellKnownResponse = AzureWellKnown.response(
                    issuer = call.request.issuer(),
                    tokenEndpoint = "$baseUrl${Konstanter.tokenPath}",
                    authorizationEndpoint = "$baseUrl${Konstanter.authorizationPath}",
                    jwksUri = "$baseUrl${Konstanter.jwksPath}"
            )
            call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = wellKnownResponse
            )
        }

        get(Konstanter.jwksPath) {
            call.respondText(
                    contentType = ContentType.Application.Json,
                    status = HttpStatusCode.OK,
                    text = Azure.V2_0.getPublicJwk()
            )
        }

        post(Konstanter.tokenPath) {
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
            val clientId = call.request.queryParameters.getOrFail("client_id")
            val redirectUri = call.request.queryParameters.getOrFail("redirect_uri")
            val scope = call.request.queryParameters.getOrFail("scope")
            val state = call.request.queryParameters.getOrFail("state")
            val nonce = call.request.queryParameters.getOrFail("nonce")

            call.respondHtml {
                head {
                    title { +"Azure Mock Login Input" }
                }
                body {
                    form(action = Konstanter.authorizationPath,
                         encType = FormEncType.applicationXWwwFormUrlEncoded,
                         method = FormMethod.post) {
                        acceptCharset = "utf-8"
                        h1 { +"Azure Mock Login Input"}
                        p {
                            label { +"Bruker ID: " }
                            textInput(name = "user_id")
                        }
                        p {
                            label { +"Navn: " }
                            textInput(name = "name")
                        }
                        hiddenInput(name = "client_id") { value = clientId }
                        hiddenInput(name = "scope") { value = scope }
                        hiddenInput(name = "state") { value = state }
                        hiddenInput(name = "nonce") { value = nonce }
                        hiddenInput(name = "redirect_uri") { value = redirectUri }
                        submitInput{ value = "Fortsett"}
                    }
                }
            }
        }

        post(Konstanter.authorizationPath) {
            val parameters = call.receiveParameters()
            val userId = parameters.getOrFail("user_id")
            val name = parameters["name"] ?: userId
            val clientId = parameters.getOrFail("client_id")
            val scope = parameters.getOrFail("scope")
            val state = parameters.getOrFail("state")
            val nonce = parameters.getOrFail("nonce")
            val redirectUri = parameters.getOrFail("redirect_uri")

            val code = AzureToken.Code(
                    userId = userId,
                    name = name,
                    scopes = scope,
                    nonce = nonce
            )

            call.respondHtml {
                head {
                    title { +"Azure Mock Login Oppsummering" }
                }
                body {
                    form(action = redirectUri,
                         encType = FormEncType.applicationXWwwFormUrlEncoded,
                         method = FormMethod.post) {
                        acceptCharset = "utf-8"
                        h1 { +"Azure Mock Login Oppsummering"}
                        p { +"Bruker ID: $userId"}
                        p { +"Navn: $name"}
                        p { +"Client ID: $clientId"}
                        p { +"Scope: $scope"}
                        p { +"State: $state"}
                        p { +"Nonce: $nonce"}
                        p { +"Redirect URI: $redirectUri"}

                        hiddenInput(name = "code") { value = code.toString() }
                        hiddenInput(name = "state") { value = state }
                        submitInput {value ="Bekreft"}
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

private fun ApplicationRequest.baseUrl() = "${origin.scheme}://${host()}:${port()}"
private fun ApplicationRequest.issuer() = "${baseUrl()}${Konstanter.basePath}"
