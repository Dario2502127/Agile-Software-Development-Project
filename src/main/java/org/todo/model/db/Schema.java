package org.todo.model.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates/updates the DB schema if missing. Call once at startup. */
public final class Schema {
	private Schema() {}

	public static void ensure() {
		try (Connection c = DB.get(); Statement st = c.createStatement()) {

			// TENDERS TABLE — covers all fields present in tender-form.html
			st.execute("""
                create table if not exists tenders(
                  id identity primary key,
                  name varchar(255) not null,
                  notice_date date not null,
                  close_date date not null,
                  disclose_date date,
                  status varchar(32) not null default 'Open',
                  staff_email varchar(255),
                  description text,
                  term_of_construction varchar(255),
                  estimated_price decimal(18,2),
                  winner_reason text,
                  winner_bid_id bigint
                );
            """);

			// BIDS TABLE
			st.execute("""
                create table if not exists bids(
                  id identity primary key,
                  tender_id bigint not null,
                  company_id varchar(64) not null,
                  company_name varchar(255) not null,
                  bid_price decimal(18,2) not null,
                  created_at timestamp not null default current_timestamp(),
                  foreign key (tender_id) references tenders(id) on delete cascade
                );
            """);

			// USEFUL INDEXES
			st.execute("create index if not exists idx_tenders_close on tenders(close_date)");
			st.execute("create index if not exists idx_bids_tender on bids(tender_id)");
		} catch (SQLException e) {
			throw new RuntimeException("Schema initialization failed", e);
		}
	}
}
