package org.todo.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.todo.model.tender.Tender;
import org.todo.model.tender.TenderDao;
import org.todo.view.TemplateEngine;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * GET  /tender-form  -> render the create form
 * POST /tender-form  -> read form fields, insert into DB via TenderDao, then redirect to /tender
 */
@WebServlet("/tender-form")
public class TenderFormServlet extends HttpServlet {

	private final TenderDao dao = new TenderDao();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Optional: only staff can see the create form. If you want truly public, remove this block.
		if (req.getSession(false) == null || req.getSession(false).getAttribute("staffId") == null) {
			resp.sendRedirect("city-login");
			return;
		}
		TemplateEngine.process("tender-form.html", req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// Optional auth check (same as in doGet)
		if (req.getSession(false) == null || req.getSession(false).getAttribute("staffId") == null) {
			resp.sendRedirect("city-login");
			return;
		}

		String name = trim(req.getParameter("name"));
		String noticeDateStr = trim(req.getParameter("noticeDate"));
		String closeDateStr  = trim(req.getParameter("closeDate"));
		String description   = trim(req.getParameter("description"));
		String term          = trim(req.getParameter("termOfConstruction"));
		String priceStr      = trim(req.getParameter("estimatedPrice"));

		if (name == null || name.isEmpty() ||
				noticeDateStr == null || closeDateStr == null) {
			req.setAttribute("message", "Name, Notice date and Close date are required.");
			TemplateEngine.process("tender-form.html", req, resp);
			return;
		}

		try {
			LocalDate notice = LocalDate.parse(noticeDateStr); // YYYY-MM-DD from <input type="date">
			LocalDate close  = LocalDate.parse(closeDateStr);

			BigDecimal price = null;
			if (priceStr != null && !priceStr.isEmpty()) {
				price = new BigDecimal(priceStr);
			}

			Tender t = new Tender();
			t.name = name;
			t.noticeDate = notice;
			t.closeDate  = close;
			t.discloseDate = null;
			t.status = "Open";
			t.staffEmail = String.valueOf(req.getSession().getAttribute("staffEmail")); // optional
			t.description = description;
			t.termOfConstruction = term;
			t.estimatedPrice = price;
			t.winnerReason = null;
			t.winnerBidId = null;

			dao.create(t);

			// Success → back to the public list
			resp.sendRedirect("tender");
		} catch (DateTimeParseException e) {
			req.setAttribute("message", "Invalid date format. Please use the date picker.");
			TemplateEngine.process("tender-form.html", req, resp);
		} catch (Exception e) {
			req.setAttribute("message", "Saving failed: " + e.getMessage());
			TemplateEngine.process("tender-form.html", req, resp);
		}
	}

	private static String trim(String s) { return s == null ? null : s.trim(); }
}
