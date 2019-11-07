package com.gitlab.ilevn.dune.routes

import com.gitlab.ilevn.dune.eval.NsJail
import com.gitlab.ilevn.dune.eval.languages
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.contentType
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.Route
import mu.KotlinLogging


@KtorExperimentalLocationsAPI
@Location("/eval")
class Eval


data class SuppliedLanguage(val code: String, val language: String)

@KtorExperimentalLocationsAPI
fun Route.apiEval(jail: NsJail) {
    val logger = KotlinLogging.logger("Eval route")

    get<Eval> {
        call.respond("OK")
    }

    post<Eval> {
        call.run {
            if (request.contentType() != ContentType.Application.Json) {
                respond(HttpStatusCode.UnsupportedMediaType, "This endpoint only supports JSON.")
            }

            receiveOrNull<SuppliedLanguage>()?.let { sup ->
                languages[sup.language.toLowerCase()]?.let {
                    jail.execute(sup.code, it)?.run {
                        respond(this)
                    } ?: respond(HttpStatusCode.InternalServerError).also {
                        logger.debug("Error, check logs.")
                    }
                } ?: respond(HttpStatusCode.BadRequest, "Unsupported language: ${sup.language}")
            }

        }
    }
}
