package sbtaptly

import java.io.File

import org.apache.http.HttpStatus
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{CloseableHttpResponse, HttpPost, HttpPut}
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import sbt.Keys._
import sbt.{AutoPlugin, Logger, SettingKey, TaskKey}

object AptlyPlugin extends AutoPlugin {

  /**
    * Defines all settings/tasks that get automatically imported,
    * when the plugin is enabled
    */
  object autoImport {
    lazy val aptlyUrl = SettingKey[String]("aptly-url", "Aptly server api http url")
    lazy val aptlyPrefix = SettingKey[String]("aptly-prefix", "The name of the debian repo in the aptly server ")
    lazy val aptlyDistribution = SettingKey[String]("aptly-distribution", "The OS distribution name of the aptly repo")
    lazy val aptlyDebianPackage = TaskKey[java.io.File]("aptly-debian-package", "The file for the debian package")

    lazy val aptlyPublish = TaskKey[Unit]("aptly-publish", "Publish debian package to aptly repo")
  }

  import autoImport._
  val httpClient = HttpClients.createDefault()
  var logger: Logger = _

  override lazy val projectSettings = Seq(
    aptlyPublish := publishAptlyPackage(aptlyUrl.value, aptlyPrefix.value, aptlyDistribution.value,
      aptlyDebianPackage.value, streams.value.log)
  )

  def publishAptlyPackage(aptlyUrl: String, aptlyPrefix: String,
                          aptlyDistribution: String, debianPackageFile: File, logger: Logger): Unit = {
    this.logger = logger

    logger.info(s"Url: $aptlyUrl aptlyPrefix: $aptlyPrefix, dist: $aptlyDistribution " +
      s"package: ${debianPackageFile.getAbsolutePath}")
    val uploadDir = "temp_upload_dir_" + java.util.UUID.randomUUID.toString

    uploadDebianFile(aptlyUrl, uploadDir, debianPackageFile) &&
      moveToRepo(aptlyUrl, aptlyPrefix, uploadDir, debianPackageFile) &&
      publishUpdate(aptlyUrl, aptlyPrefix, aptlyDistribution, debianPackageFile)
  }

  /**
    * Step 1: upload the debian package to temp location
    * http://www.aptly.info/doc/api/files/
    */
  def uploadDebianFile(baseUrl: String, uploadDir: String, debianPackageFile: File): Boolean = {

    val uploadUrl = s"$baseUrl/files/$uploadDir"
    logger.info(s"Upload url: $uploadUrl")
    val debianPackageFilePath = debianPackageFile.getAbsolutePath
    val entity = MultipartEntityBuilder.create().addBinaryBody("file", debianPackageFile).build
    val request = new HttpPost(uploadUrl)
    request.setEntity(entity)
    val requestConfig = RequestConfig.custom().setExpectContinueEnabled(true).build()
    request.setConfig(requestConfig)
    var response: CloseableHttpResponse = null
    try {
      response = httpClient.execute(request)
      response.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK =>
          logger.info(s"Successfully uploaded $debianPackageFilePath to aptly url: $uploadUrl")
          true
        case code =>
          logger.error(s"Error uploading $debianPackageFilePath to aptly url: $uploadUrl. " +
            s" [${EntityUtils.toString(response.getEntity)}]. Code: $code")
          false
      }
    } catch {
      case e: Throwable =>
        logger.error(s"Error uploading $debianPackageFilePath to aptly url: $uploadUrl. ${e.getMessage}")
        false
    } finally {
      response.close()
      request.releaseConnection()
    }
  }

  /**
    * Step 2: Copy the debian package from temp location to the repo
    * http://www.aptly.info/doc/api/repos/
    * POST /api/repos/:name/file/:dir/:file
    */
  def moveToRepo(baseUrl: String, repoPrefix: String, uploadDir: String, debianPackageFile: File): Boolean = {
    val debianPackageFilePath = debianPackageFile.getAbsolutePath
    val debianPackageFileName = debianPackageFile.getName
    val addToRepoUrl = s"$baseUrl/repos/$repoPrefix/file/$uploadDir/$debianPackageFileName"

    logger.info(s"About to add to repo url: $addToRepoUrl")
    val request = new HttpPost(addToRepoUrl)
    var response: CloseableHttpResponse = null

    try {
      response = httpClient.execute(request)
      response.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK =>
          logger.info(s"Successfully added $debianPackageFilePath to aptly repo: $repoPrefix")
          true
        case code =>
          logger.error(s"Error adding $debianPackageFilePath to aptly repo: $repoPrefix. Url: $addToRepoUrl " +
            s" [${EntityUtils.toString(response.getEntity)}]. Code: $code")
          false
      }
    } catch {
      case e: Throwable =>
        logger.error(s"Error adding $debianPackageFilePath to repo: $repoPrefix ${e.getMessage}")
        false
    } finally {
      response.close()
      request.releaseConnection()
    }
  }

  /**
    * Step: 3 repo publish update
    * http://www.aptly.info/doc/api/publish/
    * PUT /api/publish/:prefix/:distribution
    *
    * This assumes that the repo has been published earlier and is not a new one
    */
  def publishUpdate(baseUrl: String, repoPrefix: String, distribution: String,
                    debianPackageFile: File): Boolean = {
    val debianPackageFilePath = debianPackageFile.getAbsolutePath
    val publishUrl = s"$baseUrl/publish/$repoPrefix/$distribution"
    logger.info(s"About to publish to url: $publishUrl")

    val request = new HttpPut(publishUrl)
    val requestConfig = RequestConfig.custom().
      setConnectionRequestTimeout(10000).setExpectContinueEnabled(true).build()
    request.setEntity(new StringEntity("""{"ForceOverwrite": true}""", ContentType.APPLICATION_JSON))
    request.setConfig(requestConfig)

    var response: CloseableHttpResponse = null

    try {
      response = httpClient.execute(request)
      response.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK =>
          logger.info(s"Successfully published $debianPackageFilePath to aptly repo: $repoPrefix")
          true
        case code =>
          logger.error(s"Error publishing $debianPackageFilePath to aptly repo: $repoPrefix. Url: $publishUrl" +
            s" [${EntityUtils.toString(response.getEntity)}]. Code: $code")
          false
      }
    } catch {
      case e: Throwable =>
        logger.error(s"Error publishing $debianPackageFilePath to aptly repo: $repoPrefix. ${e.getMessage}")
        false
    } finally {
      request.releaseConnection()
    }
  }
}
