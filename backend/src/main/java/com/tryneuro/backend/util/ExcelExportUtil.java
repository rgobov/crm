package com.tryneuro.backend.util;

import com.tryneuro.backend.model.Appointment;
import com.tryneuro.backend.model.Contact;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportUtil {

    public static byte[] exportClients(List<Contact> contacts) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Клиенты");

            // Стили заголовков
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Границы для ячеек данных
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Шапка таблицы
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Имя", "Телефоны", "Email", "Теги (Автомобили)", "Заметки"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Заполнение данных
            int rowIdx = 1;
            for (Contact contact : contacts) {
                Row row = sheet.createRow(rowIdx++);
                
                Cell cellName = row.createCell(0);
                cellName.setCellValue(contact.getName() != null ? contact.getName() : "");
                cellName.setCellStyle(dataStyle);

                Cell cellPhones = row.createCell(1);
                cellPhones.setCellValue(contact.getPhones() != null ? String.join(", ", contact.getPhones()) : "");
                cellPhones.setCellStyle(dataStyle);

                Cell cellEmail = row.createCell(2);
                cellEmail.setCellValue(contact.getEmail() != null ? contact.getEmail() : "");
                cellEmail.setCellStyle(dataStyle);

                Cell cellTags = row.createCell(3);
                cellTags.setCellValue(contact.getTags() != null ? String.join(", ", contact.getTags()) : "");
                cellTags.setCellStyle(dataStyle);

                Cell cellNotes = row.createCell(4);
                cellNotes.setCellValue(contact.getNotes() != null ? contact.getNotes() : "");
                cellNotes.setCellStyle(dataStyle);
            }

            // Автоподбор ширины колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    public static byte[] exportAppointments(List<Appointment> appointments) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Визиты");

            // Стили заголовков
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            // Границы для ячеек данных
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Шапка таблицы
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Дата", "Время", "Клиент", "Телефон", "Услуга", "Длительность (мин)", "Статус", "Комментарий"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            // Заполнение данных
            int rowIdx = 1;
            for (Appointment app : appointments) {
                Row row = sheet.createRow(rowIdx++);

                Cell cellDate = row.createCell(0);
                cellDate.setCellValue(app.getDate() != null ? app.getDate().format(dateFormatter) : "");
                cellDate.setCellStyle(dataStyle);

                Cell cellTime = row.createCell(1);
                cellTime.setCellValue(app.getTime() != null ? app.getTime().format(timeFormatter) : "");
                cellTime.setCellStyle(dataStyle);

                Cell cellClient = row.createCell(2);
                cellClient.setCellValue(app.getClientName() != null ? app.getClientName() : "");
                cellClient.setCellStyle(dataStyle);

                Cell cellPhone = row.createCell(3);
                cellPhone.setCellValue(app.getClientPhone() != null ? app.getClientPhone() : "");
                cellPhone.setCellStyle(dataStyle);

                Cell cellService = row.createCell(4);
                cellService.setCellValue(app.getService() != null ? app.getService() : "");
                cellService.setCellStyle(dataStyle);

                Cell cellDuration = row.createCell(5);
                cellDuration.setCellValue(app.getDurationInMinutes() != null ? app.getDurationInMinutes() : 0);
                cellDuration.setCellStyle(dataStyle);

                Cell cellStatus = row.createCell(6);
                cellStatus.setCellValue(app.getStatus() != null ? app.getStatus().name() : "");
                cellStatus.setCellStyle(dataStyle);

                Cell cellComment = row.createCell(7);
                cellComment.setCellValue(app.getComment() != null ? app.getComment() : "");
                cellComment.setCellStyle(dataStyle);
            }

            // Автоподбор ширины колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
