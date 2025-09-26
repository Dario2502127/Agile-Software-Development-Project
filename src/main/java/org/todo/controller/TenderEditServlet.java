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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * GET  /tender-edit?id=123  -> prefilled edit form
 * POST /tender-edit         -> update tender 123
 */
@WebServlet("/tender-edit")
public class TenderEditServlet extends HttpServlet {

	private final TenderDao dao = new TenderDao();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// staff only
		if (req.getSession(false) == null || req.getSession(false).getAttribute("staffId") == null) {
			resp.sendRedirect("city-login");
			return;
		}

		String idParam = req.getParameter("id");
		if (idParam == null || idParam.isBlank()) {
			resp.sendRedirect("tender");
			return;
		}

		try {
			long id = Long.parseLong(idParam);
			Tender t = dao.find(id);
			if (t == null) { resp.sendRedirect("tender"); return; }

			req.setAttribute("id", String.valueOf(t.id));
			req.setAttribute("name", nz(t.name));
			req.setAttribute("notice_date", String.valueOf(t.noticeDate));
			req.setAttribute("close_date", String.valueOf(t.closeDate));
			req.setAttribute("disclose_date", t.discloseDate == null ? "" : String.valueOf(t.discloseDate));
			req.setAttribute("description", nz(t.description));
			req.setAttribute("term", nz(t.termOfConstruction));
			req.setAttribute("estimated_price", t.estimatedPrice == null ? "" : t.estimatedPrice.toPlainString());

			TemplateEngine.process("tender-edit.html", req, resp);
		} catch (NumberFormatException | SQLException e) {
			req.setAttribute("message", "Could not load tender.");
			TemplateEngine.process("tender-edit.html", req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// staff only
		if (req.getSession(false) == null || req.getSession(false).getAttribute("staffId") == null) {
			resp.sendRedirect("city-login");
			return;
		}

		String idParam       = trim(req.getParameter("id"));
		String name          = trim(req.getParameter("name"));
		String noticeStr     = trim(req.getParameter("noticeDate"));
		String closeStr      = trim(req.getParameter("closeDate"));
		String discloseStr   = trim(req.getParameter("discloseDate"));
		String description   = trim(req.getParameter("description"));
		String term          = trim(req.getParameter("termOfConstruction"));
		String priceStr      = trim(req.getParameter("estimatedPrice"));

		if (idParam == null || idParam.isBlank()) { resp.sendRedirect("tender"); return; }

		if (name == null || name.isEmpty() || noticeStr == null || closeStr == null
				|| noticeStr.isEmpty() || closeStr.isEmpty()) {
			req.setAttribute("message", "Name, Notice date and Close date are required.");
			repopulate(req);
			TemplateEngine.process("tender-edit.html", req, resp);
			return;
		}

		try {
			Tender t = dao.find(Long.parseLong(idParam));
			if (t == null) { resp.sendRedirect("tender"); return; }

			t.name = name;
			t.noticeDate = LocalDate.parse(noticeStr);
			t.closeDate = LocalDate.parse(closeStr);
			t.discloseDate = (discloseStr == null || discloseStr.isEmpty()) ? null : LocalDate.parse(discloseStr);
			// do not touch status/staff/winner fields here
			t.description = description;
			t.termOfConstruction = term;
			t.estimatedPrice = (priceStr == null || priceStr.isEmpty()) ? null : new BigDecimal(priceStr);

			dao.update(t);
			resp.sendRedirect("tender-detail?id=" + t.id);
		} catch (DateTimeParseException e) {
			req.setAttribute("message", "Invalid date format. Please use the date picker.");
			repopulate(req);
			TemplateEngine.process("tender-edit.html", req, resp);
		} catch (Exception e) {
			req.setAttribute("message", "Saving failed: " + e.getMessage());
			repopulate(req);
			TemplateEngine.process("tender-edit.html", req, resp);
		}
	}

	private static void repopulate(HttpServletRequest req) {
		req.setAttribute("id", trim(req.getParameter("id")));
		req.setAttribute("name", trim(req.getParameter("name")));
		req.setAttribute("notice_date", trim(req.getParameter("noticeDate")));
		req.setAttribute("close_date", trim(req.getParameter("closeDate")));
		req.setAttribute("disclose_date", trim(req.getParameter("discloseDate")));
		req.setAttribute("description", trim(req.getParameter("description")));
		req.setAttribute("term", trim(req.getParameter("termOfConstruction")));
		req.setAttribute("estimated_price", trim(req.getParameter("estimatedPrice")));
	}

	private static String trim(String s) { return s == null ? null : s.trim(); }
	private static String nz(String s) { return s == null ? "" : s; }
}
