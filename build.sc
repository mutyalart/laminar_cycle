import mill._, scalajslib._, scalalib._, publish._

object meta {
  val scalaVersion   = "2.12.11"
  val scalaJSVersion = "1.0.1"

  val laminarVersion = "0.9.0"
  val zioVersion     = "1.0.0-RC19"

  val publishVersion = os.read(os.pwd / "VERSION").trim
  val pomSettings =
    PomSettings(
      description = "Cycle style FRP interfaces using Laminar",
      organization = "com.github.vic.laminar.cycle",
      url = "https://github.com/vic/laminar_cycle",
      licenses = Seq(License.`Apache-2.0`),
      versionControl = VersionControl.github("vic", "laminar_cycle"),
      developers = Seq(
        Developer("vic", "Victor Borja", "https://github.com/vic")
      )
    )

  object deps {
    val laminar = ivy"com.raquo::laminar::${laminarVersion}"
    val zio = ivy"dev.zio::zio::${zioVersion}"
    val zioStreams = ivy"dev.zio::zio-streams::${zioVersion}"
  }
}

trait BaseModule extends ScalaJSModule {
  override def scalaVersion   = meta.scalaVersion
  override def scalaJSVersion = meta.scalaJSVersion
}

object cycle extends BaseModule with PublishModule {
  override def artifactName = "cycle"
  def publishVersion        = T { meta.publishVersion }
  def pomSettings           = T { meta.pomSettings }
  override def ivyDeps      = super.ivyDeps() ++ Agg(meta.deps.laminar, meta.deps.zio)
}

object drivers extends Module {

  trait Driver extends BaseModule with PublishModule {
    def publishVersion      = T { meta.publishVersion }
    def pomSettings         = T { meta.pomSettings }
    override def moduleDeps = super.moduleDeps ++ Seq(cycle)
    override def artifactName = s"driver-${millModuleBasePath.value.last}"
  }

  object all extends Driver {
    override def artifactName = "all-drivers"
    override def moduleDeps   = super.moduleDeps ++ Seq(
      fetch, zio, topic, combine, state
    )

    def mdocProperties = T {
      val cp = (
        runClasspath()
      ).map(_.path.toString).mkString(":")
      val jsc = scalacPluginClasspath().map(_.path.toString).find(_.contains("scalajs-compiler")).get
      val mdoc =
        s"""
          |js-classpath=${cp}
          |js-scalac-options=-Xplugin:${jsc}
          |""".stripMargin.trim
      os.makeDir.all(T.ctx().dest)
      os.write(T.ctx().dest / "mdoc.properties", mdoc)
    }
  }

  object fetch extends Driver

  object zio extends Driver {
    override def ivyDeps      = super.ivyDeps() ++ Agg(meta.deps.zioStreams)
  }

  object topic extends Driver

  object combine extends Driver

  object state extends Driver
}

object examples extends Module {

  trait Example extends BaseModule {
    override def moduleDeps = super.moduleDeps :+ drivers.all
  }

  object cycle_counter extends Example
  object swapi_driver  extends Example

}
