package bank.http

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import bank.actors.Command
import bank.actors.Response
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.Future
import scala.concurrent.duration._

case class BankAccountCreateRequest(user: String, currency: String, initialBalance: Double) {
  def toCommand(replyTo: ActorRef[Response]): Command =
    Command.CreateBankAccount(user, currency, initialBalance, replyTo)
}

case class BankAccountUpdateRequest(currency: String, amount: Double) {
  def toCommand(id: String, replyTo: ActorRef[Response]): Command =
    Command.UpdateBalance(id, currency, amount, replyTo)
}

case class FailureResponse(message: String)

class BankRouter(bank: ActorRef[Command])(implicit system: akka.actor.typed.ActorSystem[_]) {

  implicit val timeout: Timeout = Timeout(5.seconds)

  def createBankAccount(request: BankAccountCreateRequest): Future[Response] =
    bank.ask(replyTo => request.toCommand(replyTo))

  def getBankAccount(id: String): Future[Response] =
    bank.ask(replyTo => Command.GetBankAccount(id, replyTo))

  def updateBankAccount(id: String, request: BankAccountUpdateRequest): Future[Response] =
    bank.ask(replyTo => request.toCommand(id, replyTo))

  /**
    * POST /banks
    *   Payload: bank account creation request as JSON
    *   Response:
    *     201 Created
    *     Location: /bank/uuid
    * 
    * GET /banks/uuid
    *   Response:
    *   - 200 OK
    *     JSON representation of bank account
    *   - 404 Not Found
    * 
    * PUT /banks/uuid
    *   Payload: (current, amount) as JSON
    *   Response:
    *   - 200 OK
    *     Payload: (new) bankaccount as JSON
    *   - 404 Not Found
    *   - 400 Bad Request
    *   - TODO
    * 
    */

  val routes =
    pathPrefix("banks") {
      pathEndOrSingleSlash {
        post {
          // parse the payload
          entity(as[BankAccountCreateRequest]) { request =>
            // convert the request into a command
            // send the command to the bank actor
            // expect a reply
            // send back an HTTP response
            onSuccess(createBankAccount(request)) {
              case Response.BankAccountCreatedResponse(id) =>
                respondWithHeader(Location(s"/banks/$id")) {
                  complete(StatusCodes.Created)
                }
              case _ =>
                complete(StatusCodes.InternalServerError)
            }
          }
        } ~
          path(Segment) { id =>
            get {
              // send command to the bank
              // expect a reply
              // send back an HTTP response
              onSuccess(getBankAccount(id)) {
                case Response.GetBankAccountResponse(Some(account)) =>
                  complete(account)
                case Response.GetBankAccountResponse(None) =>
                  complete(
                    StatusCodes.NotFound,
                    FailureResponse(s"Bank account with ID $id not found")
                  )
                case _ =>
                  complete(StatusCodes.InternalServerError)
              }
            } ~
              put {
                // parse the payload
                entity(as[BankAccountUpdateRequest]) { request =>
                  // convert the request into a command
                  // send the command to the bank actor
                  // expect a reply
                  // send back an HTTP response

                  // TODO validate the request
                  onSuccess(updateBankAccount(id, request)) {
                    case Response.BankAccountBalanceUpdatedResponse(Some(account)) =>
                      complete(account)
                    case Response.BankAccountBalanceUpdatedResponse(None) =>
                      complete(
                        StatusCodes.NotFound,
                        FailureResponse(s"Bank account with ID $id not found")
                      )
                    case _ =>
                      complete(StatusCodes.InternalServerError)
                  }
                }
              }
          }
      }
    }
}
