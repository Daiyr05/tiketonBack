package org.example.lateticket.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.example.lateticket.entity.Ticket;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfTicketService {

    private final QrCodeService qrCodeService;

    public byte[] generateTicketPdf(Ticket ticket) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, Color.BLACK);
            Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD, Color.DARK_GRAY);
            Font normalFont = new Font(Font.HELVETICA, 12, Font.NORMAL, Color.BLACK);

            // Title
            Paragraph title = new Paragraph("BiletFlow - Event Ticket", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("Event: " + ticket.getEvent().getTitle(), headerFont));
            document.add(new Paragraph(" "));

            if (ticket.getEvent().getEventDate() != null) {
                document.add(new Paragraph("Date: " +
                        ticket.getEvent().getEventDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                        normalFont));
            }
            if (ticket.getEvent().getVenueName() != null) {
                document.add(new Paragraph("Venue: " + ticket.getEvent().getVenueName(), normalFont));
            }
            if (ticket.getEvent().getVenueAddress() != null) {
                document.add(new Paragraph("Address: " + ticket.getEvent().getVenueAddress(), normalFont));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Ticket Type: " + ticket.getTicketType().getName(), normalFont));
            document.add(new Paragraph("Attendee: " + ticket.getAttendeeName(), normalFont));
            document.add(new Paragraph("Ticket ID: " + ticket.getTicketCode(), normalFont));

            if (ticket.getSeatSection() != null) {
                document.add(new Paragraph("Section: " + ticket.getSeatSection() +
                        " | Row: " + ticket.getSeatRow() +
                        " | Seat: " + ticket.getSeatNumber(), normalFont));
            }

            document.add(new Paragraph(" "));

            // QR Code
            byte[] qrBytes = qrCodeService.generateQrCode(ticket.getTicketCode(), 250, 250);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            document.add(qrImage);

            document.add(new Paragraph(" "));
            Paragraph footer = new Paragraph("Present this ticket at the entrance for verification.",
                    new Font(Font.HELVETICA, 10, Font.ITALIC, Color.GRAY));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF ticket", e);
        }
    }
}