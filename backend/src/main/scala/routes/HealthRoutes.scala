package routes

object HealthRoutes extends BaseRoutes:

  @cask.get("/health")
  def health() =
    ok(ujson.Obj("status" -> "ok"))

  initialize()
