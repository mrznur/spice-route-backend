package com.spiceroute.feedback.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.spiceroute.feedback.common.ApiException;
import com.spiceroute.feedback.domain.DiningTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class QrService {

    private static final int PDF_COLUMNS = 2;
    private static final float CARD_HEIGHT = 375f;

    private final String publicBaseUrl;

    public QrService(@Value("${PUBLIC_BASE_URL:http://localhost:3000}") String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    /** The URL a customer lands on after scanning: {frontend}/f/{qrToken} */
    public String feedbackLink(String qrToken) {
        return publicBaseUrl + "/f/" + qrToken;
    }

    public byte[] png(String qrToken, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(feedbackLink(qrToken), BarcodeFormat.QR_CODE, size, size,
                    Map.of(EncodeHintType.MARGIN, 1,
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.CHARACTER_SET, "UTF-8"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate QR code");
        }
    }

    /** A4 pages, 2 x 2 cards per page: restaurant, branch, table label, QR code, short call to action. */
    public byte[] branchPdf(String restaurantName, String branchName, List<DiningTable> tables) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            PdfPTable grid = new PdfPTable(PDF_COLUMNS);
            grid.setWidthPercentage(100);
            for (DiningTable table : tables) {
                grid.addCell(card(restaurantName, branchName, table));
            }
            // pad the last row so every row has PDF_COLUMNS cells
            int remainder = tables.size() % PDF_COLUMNS;
            if (remainder != 0) {
                for (int i = remainder; i < PDF_COLUMNS; i++) {
                    PdfPCell empty = new PdfPCell();
                    empty.setBorder(Rectangle.NO_BORDER);
                    empty.setFixedHeight(CARD_HEIGHT);
                    grid.addCell(empty);
                }
            }
            document.add(grid);
            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate PDF");
        }
    }

    private PdfPCell card(String restaurantName, String branchName, DiningTable table)
            throws DocumentException, IOException {
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(CARD_HEIGHT);
        cell.setPadding(14f);
        cell.setBorderColor(new Color(0xCC, 0xCC, 0xCC));

        cell.addElement(centered(restaurantName, FontFactory.HELVETICA_BOLD, 17f, 2f));
        cell.addElement(centered(branchName, FontFactory.HELVETICA, 12f, 8f));
        cell.addElement(centered(table.getLabel(), FontFactory.HELVETICA_BOLD, 26f, 8f));

        Image qr = Image.getInstance(png(table.getQrToken(), 600));
        qr.scaleToFit(230f, 230f);
        qr.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(qr);

        cell.addElement(centered("Scan to share your feedback", FontFactory.HELVETICA, 11f, 0f));
        return cell;
    }

    private Paragraph centered(String text, String fontName, float size, float spacingAfter) {
        Paragraph p = new Paragraph(text, FontFactory.getFont(fontName, size));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(spacingAfter);
        return p;
    }
}
