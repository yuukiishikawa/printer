import java.io.File
import java.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import jp.asaas.taxschema.itdef.ver13._
import jp.asaas.taxschema.shotoku.form.ver130._
import org.apache.pdfbox.multipdf.Overlay
import org.apache.pdfbox.pdmodel.font.{PDFont, PDType0Font}
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Future


object PrinterMain {

  import scala.concurrent.ExecutionContext.Implicits._

  def main(args: Array[String]): Unit = {


    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    val path = "/Users/ishikawayuuki/Downloads/"

    val wsClient = AhcWSClient()

    call(wsClient)
      .andThen { case _ => wsClient.close() }
      .andThen { case _ => system.terminate() }

    def call(wsClient: WSClient): Future[Unit] = {
      wsClient.url("http://dev-proto01.a-saas.io:9000/RKO0010/KOA020_1/13").withHeaders("X-Api-Key" -> "any").get().map { response =>
        validateFormJson[KOA020_1Element](response.body) match {
          case Some(form) => {

            val overlayTextDoc = new PDDocument
            val page = new PDPage
            overlayTextDoc.addPage(page)

            val contentStream = new PDPageContentStream(overlayTextDoc, page, PDPageContentStream.AppendMode.APPEND, false, false)

            val ttf = new File(path + "IPAfont00303/ipag.ttf")
            val font = PDType0Font.load(overlayTextDoc, ttf)

            val elementMap = KOA020_1ToElementMap(form)
            println(elementMap.toString)

            printToOverlay(elementMap.get("aba00010/yy/yy"))(200, 758)(font, 14)(contentStream)
            printToOverlay(elementMap.get("aba00030/zeimusho/zeimusho_NM"))(70, 770)(font, 7)(contentStream)
            printToOverlay(elementMap.get("aba00040/yymmdd/yy"))(73, 758)(font, 8)(contentStream)
            printToOverlay(elementMap.get("aba00040/yymmdd/mm"))(99, 758)(font, 8)(contentStream)
            printToOverlay(elementMap.get("aba00040/yymmdd/dd"))(127, 758)(font, 8)(contentStream)

            printToOverlay(elementMap.get("aba00080/zipcode/zip1"))(112, 736)(font, 13)(contentStream)
            printToOverlay(elementMap.get("aba00080/zipcode/zip2"))(160, 736)(font, 13)(contentStream)
            printToOverlay(elementMap.get("aba00090/address"))(105, 713)(font, 13)(contentStream)
            printToOverlay(elementMap.get("aba00095/address"))(105, 690)(font, 13)(contentStream)

            printToOverlay(elementMap.get("aba000120/address"))(105, 680)(font, 11)(contentStream)


            printToOverlay(elementMap.get("aba00130/nkana"))(330, 736)(font, 13)(contentStream)

            printToOverlay(elementMap.get("aba00140/name"))(330, 710)(font, 15)(contentStream)

            printToOverlay(elementMap.get("aba00160/shokugyo"))(330, 687)(font, 6)(contentStream)
            printToOverlay(elementMap.get("aba00170/yago"))(396, 687)(font, 5)(contentStream)
            printToOverlay(elementMap.get("aba00180/name"))(464, 687)(font, 6)(contentStream)
            printToOverlay(elementMap.get("aba00190/zokugara"))(524, 687)(font, 6)(contentStream)

            printToOverlay(elementMap.get("aba00200/yymmdd/yy"))(342, 670)(font, 12)(contentStream)
            printToOverlay(elementMap.get("aba00200/yymmdd/mm"))(378, 670)(font, 12)(contentStream)
            printToOverlay(elementMap.get("aba00200/yymmdd/dd"))(415, 670)(font, 12)(contentStream)

            printToOverlay(elementMap.get("aba00220/telnumber/tel1"))(470, 668)(font, 10)(contentStream)
            printToOverlay(elementMap.get("aba00220/telnumber/tel2"))(500, 668)(font, 10)(contentStream)
            printToOverlay(elementMap.get("aba00220/telnumber/tel2"))(530, 668)(font, 10)(contentStream)

            printToOverlay(if (elementMap.get("aba00240/kubun/kubun_CD").getOrElse(-1) == "1") Some("◯") else None)(202, 650)(font, 11)(contentStream)
            printToOverlay(if (elementMap.get("aba00250/kubun/kubun_CD").getOrElse(-1) == "1") Some("◯") else None)(222, 650)(font, 11)(contentStream)
            printToOverlay(if (elementMap.get("aba00252/kubun/kubun_CD").getOrElse(-1) == "1") Some("◯") else None)(243, 650)(font, 11)(contentStream)
            printToOverlay(if (elementMap.get("aba00253/kubun/kubun_CD").getOrElse(-1) == "1") Some("◯") else None)(264, 650)(font, 11)(contentStream)
            printToOverlay(if (elementMap.get("aba00253/kubun/kubun_CD").getOrElse(-1) == "1") Some("◯") else None)(284, 650)(font, 11)(contentStream)


            printToOverlay(elementMap.get("abb00030/kingaku"))(210, 627)(font, 14)(contentStream)
            printToOverlay(elementMap.get("abb00040/kingaku"))(210, 610)(font, 14)(contentStream)
            printToOverlay(elementMap.get("abb00050/kingaku"))(210, 593)(font, 14)(contentStream)
            printToOverlay(elementMap.get("abb00060/kingaku"))(210, 576)(font, 14)(contentStream)
            printToOverlay(elementMap.get("abb00070/kingaku"))(210, 559)(font, 14)(contentStream)
            printToOverlay(elementMap.get("abb00080/kingaku"))(210, 542)(font, 14)(contentStream)


            contentStream.close()


            val taxDoc = PDDocument.load(new File(path + "02_1.pdf"))
            val overlayGuide = new util.HashMap[Integer, String]()
            val overlay = new Overlay()
            overlay.setInputPDF(taxDoc)
            overlay.setOverlayPosition(Overlay.Position.FOREGROUND)
            overlay.setAllPagesOverlayPDF(overlayTextDoc)

            val finaldoc = overlay.overlay(overlayGuide)

            finaldoc.save(path + "final.pdf")

            finaldoc.close
            overlay.close

            overlayTextDoc.close()
          }
          case None => ;
        }
      }
    }

    def validateFormJson[T](s: String)(implicit rds: Reads[T], wts: Writes[T]) = {
      Json.parse(s).validate[T].fold(
        errors => None,
        readValue => Option(readValue))
    }


  }

  def printToOverlay(maybeText: Option[String])(t: Int, s: Int)(font: PDFont, fontSize: Int)(contentStream: PDPageContentStream) = {

    maybeText match {
      case Some(text) => {
        contentStream.beginText()
        contentStream.setFont(font, fontSize)

        //Setting the position for the line
        contentStream.newLineAtOffset(t, s)
        contentStream.showText(text)
        contentStream.endText()
      }
      case None =>
    }

  }

  def KOA020_1ToElementMap(e: KOA020_1Element) = {

    itdefElementMap("aba00010", e.aba00010) ++
      itdefElementMap("aba00020", e.aba00020) ++
      itdefElementMap("aba00030", e.aba00030) ++
      itdefElementMap("aba00040", e.aba00040) ++
      itdefElementMap("aba00070", e.aba00070) ++
      itdefElementMap("aba00080", e.aba00080) ++
      itdefElementMap("aba00090", e.aba00090) ++
      itdefElementMap("aba00095", e.aba00095) ++
      itdefElementMap("aba00110", e.aba00110) ++
      itdefElementMap("aba00120", e.aba00120) ++
      itdefElementMap("aba00130", e.aba00130) ++
      itdefElementMap("aba00140", e.aba00140) ++
      itdefElementMap("aba00150", e.aba00150) ++
      itdefElementMap("aba00160", e.aba00160) ++
      itdefElementMap("aba00170", e.aba00170) ++
      itdefElementMap("aba00180", e.aba00180) ++
      itdefElementMap("aba00190", e.aba00190) ++
      itdefElementMap("aba00200", e.aba00200) ++
      itdefElementMap("aba00220", e.aba00220) ++
      itdefElementMap("aba00240", e.aba00240) ++
      itdefElementMap("aba00250", e.aba00250) ++
      itdefElementMap("aba00252", e.aba00252) ++
      itdefElementMap("aba00253", e.aba00253) ++
      itdefElementMap("aba00256", e.aba00256) ++
      itdefElementMap("aba00260", e.aba00260) ++
      itdefElementMap("abb00030", e.abb00030) ++
      itdefElementMap("abb00040", e.abb00040) ++
      itdefElementMap("abb00050", e.abb00050) ++
      itdefElementMap("abb00060", e.abb00060) ++
      itdefElementMap("abb00070", e.abb00070) ++
      itdefElementMap("abb00080", e.abb00080) ++
      itdefElementMap("abb00100", e.abb00100) ++
      itdefElementMap("abb00110", e.abb00110) ++
      itdefElementMap("abb00130", e.abb00130) ++
      itdefElementMap("abb00180", e.abb00180) ++
      itdefElementMap("abb00230", e.abb00230) ++
      itdefElementMap("abb00290", e.abb00290) ++
      itdefElementMap("abb00300", e.abb00300) ++
      itdefElementMap("abb00310", e.abb00310) ++
      itdefElementMap("abb00320", e.abb00320) ++
      itdefElementMap("abb00330", e.abb00330) ++
      itdefElementMap("abb00340", e.abb00340) ++
      itdefElementMap("abb00350", e.abb00350) ++
      itdefElementMap("abb00360", e.abb00360) ++
      itdefElementMap("abb00370", e.abb00370) ++
      itdefElementMap("abb00380", e.abb00380) ++
      itdefElementMap("abb00390", e.abb00390) ++
      itdefElementMap("abb00400", e.abb00400) ++
      itdefElementMap("abb00410", e.abb00410) ++
      itdefElementMap("abb00430", e.abb00430) ++
      itdefElementMap("abb00440", e.abb00440) ++
      itdefElementMap("abb00450", e.abb00450) ++
      itdefElementMap("abb00460", e.abb00460) ++
      itdefElementMap("abb00470", e.abb00470) ++
      itdefElementMap("abb00480", e.abb00480) ++
      itdefElementMap("abb00490", e.abb00490) ++
      itdefElementMap("abb00500", e.abb00500) ++
      itdefElementMap("abb00510", e.abb00510) ++
      itdefElementMap("abb00515", e.abb00515) ++
      itdefElementMap("abb00520", e.abb00520) ++
      itdefElementMap("abb00540", e.abb00540) ++
      itdefElementMap("abb00550", e.abb00550) ++
      itdefElementMap("abb00560", e.abb00560) ++
      itdefElementMap("abb00580", e.abb00580) ++
      itdefElementMap("abb00590", e.abb00590) ++
      itdefElementMap("abb00600", e.abb00600) ++
      itdefElementMap("abb00620", e.abb00620) ++
      itdefElementMap("abb00630", e.abb00630) ++
      itdefElementMap("abb00640", e.abb00640) ++
      itdefElementMap("abb00645", e.abb00645) ++
      itdefElementMap("abb00650", e.abb00650) ++
      itdefElementMap("abb00660", e.abb00660) ++
      itdefElementMap("abb00970", e.abb00970) ++
      itdefElementMap("abb00980", e.abb00980) ++
      itdefElementMap("abb00990", e.abb00990) ++
      itdefElementMap("abb01000", e.abb01000) ++
      itdefElementMap("abb00663", e.abb00663) ++
      itdefElementMap("abb00665", e.abb00665) ++
      itdefElementMap("abb00670", e.abb00670) ++
      itdefElementMap("abb00676", e.abb00676) ++
      itdefElementMap("abb00680", e.abb00680) ++
      itdefElementMap("abb01010", e.abb01010) ++
      itdefElementMap("abb01020", e.abb01020) ++
      itdefElementMap("abb01030", e.abb01030) ++
      itdefElementMap("abb01040", e.abb01040) ++
      itdefElementMap("abb00710", e.abb00710) ++
      itdefElementMap("abb00720", e.abb00720) ++
      itdefElementMap("abb00730", e.abb00730) ++
      itdefElementMap("abb00750", e.abb00750) ++
      itdefElementMap("abb00760", e.abb00760) ++
      itdefElementMap("abb00780", e.abb00780) ++
      itdefElementMap("abb00790", e.abb00790) ++
      itdefElementMap("abb00800", e.abb00800) ++
      itdefElementMap("abb00810", e.abb00810) ++
      itdefElementMap("abb00820", e.abb00820) ++
      itdefElementMap("abb00830", e.abb00830) ++
      itdefElementMap("abb00840", e.abb00840) ++
      itdefElementMap("abb00860", e.abb00860) ++
      itdefElementMap("abb00870", e.abb00870) ++
      itdefElementMap("abb00890", e.abb00890) ++
      itdefElementMap("abb00900", e.abb00900) ++
      itdefElementMap("abb00950", e.abb00950) ++
      itdefElementMap("abs00010", e.abs00010) ++
      itdefElementMap("abs00020", e.abs00020) ++
      itdefElementMap("abs00030", e.abs00030) ++
      itdefElementMap("abs00040", e.abs00040)
  }


  def itdefElementMap[T](parentTag: String, t: T): Map[String, String] = {

    def simplePath[C](tag: String, c: C) = tag + "/" + c.getClass.getSimpleName.toLowerCase()

    t match {
      case Some(s: Kubun2) =>
        val map1 = Map(s"${simplePath(parentTag, s)}/kubun_CD" -> s.kubun_CD.toString)
        val map2 = s.kubun_NM match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/kubun_NM" -> u)
        }
        map1 ++ map2

      case Some(s: Kubun) =>
        val map1 = Map(s"${simplePath(parentTag, s)}/kubun_CD" -> s.kubun_CD.toString)
        val map2 = s.kubun_NM match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/kubun_NM" -> u)
        }
        map1 ++ map2

      case Some(s: Zeimusho) =>

        val map1 = Map(s"${simplePath(parentTag, s)}/zeimusho_CD" -> s.zeimusho_CD)
        val map2 = s.zeimusho_NM match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/zeimusho_NM" -> u)
        }
        map1 ++ map2

      case Some(s: Yymmdd) =>
        val map1 = Map(
          s"${simplePath(parentTag, s)}/era" -> s.era.toString,
          s"${simplePath(parentTag, s)}/yy" -> s.yy.toString,
          s"${simplePath(parentTag, s)}/mm" -> s.mm.toString,
          s"${simplePath(parentTag, s)}/dd" -> s.dd.toString)
        map1

      case Some(s: ZipCode) =>
        val map1 = Map(
          s"${simplePath(parentTag, s)}/zip1" -> s.zip1,
          s"${simplePath(parentTag, s)}/zip2" -> s.zip2)
        map1

      case Some(s: Address) =>
        val map1 = Map(s"${simplePath(parentTag, s)}" -> s.address)
        map1

      case Some(s: NKana) =>
        val map1 = Map(s"${simplePath(parentTag, s)}" -> s.nkana)
        map1

      case Some(s: Name) =>
        val map1 = Map(s"${simplePath(parentTag, s)}" -> s.name)
        map1

      case Some(s: Shokugyo) =>
        val map1 = Map(s"${simplePath(parentTag, s)}" -> s.shokugyo)
        map1

      case Some(s: Yago) =>
        val map1 = Map(s"${simplePath(parentTag, s)}" -> s.yago)
        map1

      case Some(s: Zokugara) =>
        val map1 = Map(s"${simplePath(parentTag, s)}" -> s.zokugara)
        map1

      case Some(s: Yy) =>
        val map1 = Map(
          s"${simplePath(parentTag, s)}/era" -> s.era.toString,
          s"${simplePath(parentTag, s)}/yy" -> s.yy.toString)
        map1

      case Some(s: TelNumber) =>

        val map1 = s.tel1 match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/tel1" -> u)
        }
        val map2 = s.tel2 match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/tel2" -> u)
        }
        val map3 = Map(s"${simplePath(parentTag, s)}/tel3" -> s.tel3)
        map1 ++ map2 ++ map3

      case Some(s: Kingaku) =>
        val map1 = Map(
          s"${simplePath(parentTag, s)}" -> s.kingaku.toString)
        map1

      case Some(s: Account) =>

        val map1 = s.kinyukikan_NM match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/kinyukikan_NM" -> u)
        }
        val map2 = s.shiten_NM match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/shiten_NM" -> u)
        }
        val map3 = s.kinyukikan_CD match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/kinyukikan_CD" -> u)
        }
        val map4 = s.shiten_CD match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/shiten_CD" -> u)
        }
        val map5 = s.yokin match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/yokin" -> u.toString)
        }
        val map6 = s.koza match {
          case Some(u) => Map(s"${simplePath(parentTag, s)}/koza" -> u)
        }

        map1 ++ map2 ++ map3 ++ map4 ++ map5 ++ map6

      case Some(s: String) =>
        val map1 = Map(
          s"${simplePath(parentTag, s)}" -> s)
        map1

      case Some(s: Int) =>
        val map1 = Map(
          s"${simplePath(parentTag, s)}" -> s.toString)
        map1

      case None => Map("${simplePath(parentTag, s)}" -> "error")
    }


  }


  /*2
 aba00010: Option[Yy], // 年分 : NENBUN
* */


}


