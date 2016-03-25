package xls

import java.io.File

import common.BaseObject
import entities.{Transactions, Contact}
import org.apache.poi.ss.usermodel._
import utils.DateUtils
import scala.collection.JavaConversions._

/**
 * Created by hkatz on 3/17/16.
 */

object ProcessXLS {

  def parseRow(row: Row): BaseObject = {
    val rowSz = row.size
    Contact.empty
  }

  def readFile(xlsFile: File, userid: Int, uploadType: String) = {
    val wb = WorkbookFactory.create(xlsFile)

    def getCellString(cell: Cell) = {
      cell.getCellType() match {
        case Cell.CELL_TYPE_NUMERIC =>
          (new DataFormatter()).formatCellValue(cell)
        case Cell.CELL_TYPE_STRING =>
          cell.getStringCellValue()
        case Cell.CELL_TYPE_FORMULA =>
          val evaluator = wb.getCreationHelper().createFormulaEvaluator()
          (new DataFormatter()).formatCellValue(cell, evaluator)
        case _ => " "
      }
    }

    val sheet = wb.getSheetAt(0)

    val rows = sheet.rowIterator.size

    val types:List[BaseObject] = {
      for {
        row <- sheet.rowIterator()
        cells = row.cellIterator.map(getCellString).toList
        nCells = cells.size
        obj = uploadType match {
          case "C" =>
            Contact.apply(Some(cells(0)),
              if (nCells > 1) Some(cells(1)) else None,
              if (nCells > 2) Some(cells(2)) else None,
              if (nCells > 3) Some(cells(3)) else None,
              if (nCells > 4) Some(cells(4)) else None,
              if (nCells > 5) Some(cells(5)) else None,
              Some(userid.toString))
          case "E" =>
            val trandate = DateUtils.dateParse(cells(0), DateUtils.YMD)
            Transactions.apply(trandate.toDate,
              if (nCells > 1) Some(cells(1)) else None,
              if (nCells > 2) Some(cells(2)) else None,
              if (nCells > 3) Some(cells(3)) else None,
              if (nCells > 4) Some(cells(4)) else None,
              if (nCells > 5) Some(cells(5)) else None,
              if (nCells > 6) Some(cells(6)) else None,
              if (nCells > 7) Some(cells(7).toDouble) else None,
              if (nCells > 8) Some(cells(8).toDouble) else None,
              if (nCells > 9) Some(cells(9)) else None,
              userid)
        }
      } yield obj
    }.toList.tail  // drop header line

    types.foreach { baseobj =>
      uploadType match {
        case "C" =>
          val myContact = baseobj.asInstanceOf[Contact]
          val matched = Contact.findBiz(myContact.bizname, userid)
          if (matched.isEmpty) Contact.save(myContact)
        case "E" =>
          val myTrans = baseobj.asInstanceOf[Transactions]
          val matched = Transactions.findBiz(myTrans.trantype, userid)
          if (matched.isEmpty) Transactions.save(myTrans)
      }
    }
    val text = sheet.rowIterator.map { row: Row =>
      row.cellIterator.map(getCellString).mkString("|")
    }.mkString("\n")
  }

}