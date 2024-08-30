
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class ExcelHelper {

    public byte[] generateXlsx(List<?> list, String[] header, String[] beanMethods) {

        byte[] xlsx = null;

        try {

            if (list != null && list.size() != 0) {

//				Creating Xlsx workbook and sheet
                SXSSFWorkbook swb = new SXSSFWorkbook();
                Sheet sheet = swb.createSheet();

//				Creating header
                Row rowhead = sheet.createRow(0);
                CellStyle cellStyle = swb.createCellStyle();
                cellStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
                cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                for (int i = 0; i < header.length; i++) {
                    Cell cellA1 = rowhead.createCell(i);
                    cellA1.setCellStyle(cellStyle);
                    cellA1.setCellValue(header[i]);
                }

//				Getting class of object
                Class clsObj = list.get(0).getClass();

//				Looping over each row in the list
                for(int rowNumber = 0; rowNumber < list.size(); rowNumber++){

                    Row currentRow = sheet.createRow(rowNumber + 1);
//					Looping over each column in the list
                    for (int i = 0; i < beanMethods.length; i++) {
                        Object obj = null;
                        try {
                            Field fld = clsObj.getDeclaredField(beanMethods[i]);
                            fld.setAccessible(true);
                            obj = fld.get(list.get(rowNumber));
//				System.out.println(beanMethods[i] + " Bean value: " + obj.toString());
                        } catch (Exception e) {
//				log.error("Unable to find the dto class field: " + beanMethods[i] + "; Error Message: " + e.getMessage());
                        }
                        if (obj != null) {
                            currentRow.createCell(i).setCellValue(obj.toString());
                        }
                    }

                }

                ByteArrayOutputStream fout = new ByteArrayOutputStream();
                swb.write(fout);
                fout.close();

                xlsx = fout.toByteArray();
                return xlsx;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return xlsx;

    }

    public SXSSFSheet generateXLSXWithHeader(String[] header, SXSSFWorkbook swb) {

        SXSSFSheet sheet = swb.createSheet();

        Row rowhead = sheet.createRow(0);

        CellStyle cellStyle = swb.createCellStyle();
        cellStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        for (int i = 0; i < header.length; i++) {
            Cell cellA1 = rowhead.createCell(i);
            cellA1.setCellStyle(cellStyle);
            cellA1.setCellValue(header[i]);
        }
        return sheet;
    }

    public void addRowXLSX(String[] beanMethods, Sheet sheet, Object dto, int rowNumber) {
        Row currentRow = sheet.createRow(rowNumber);
        for (int i = 0; i < beanMethods.length; i++) {
            Object obj = null;
            try {
                Field fld = dto.getClass().getDeclaredField(beanMethods[i]);
                fld.setAccessible(true);
                obj = fld.get(dto);
//				System.out.println(beanMethods[i] + " Bean value: " + obj.toString());
            } catch (Exception e) {
//				log.error("Unable to find the dto class field: " + beanMethods[i] + "; Error Message: " + e.getMessage());
            }
            if (obj != null) {
                currentRow.createCell(i).setCellValue(obj.toString());
            }
        }
    }

    public void addCellStyle(SXSSFWorkbook swb, Sheet sheet, int noOfCol, int rowNumber){
        // Styling specific to CRB Form
        CellStyle cellStyle = swb.createCellStyle();
        cellStyle.setWrapText(true);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CellStyle rowCellStyle = swb.createCellStyle();
        rowCellStyle.setAlignment(HorizontalAlignment.CENTER);
        rowCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        rowCellStyle.setWrapText(true);
        IntStream.range(0, noOfCol).forEach((i)-> {
            sheet.setColumnWidth(i, 256*20 );
            if(sheet.getRow(rowNumber).getCell(i)!=null){
                sheet.getRow(rowNumber).getCell(i).setCellStyle(rowCellStyle);
            }
        });

    }
}
