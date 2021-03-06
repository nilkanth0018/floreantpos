/**
 * ************************************************************************
 * * The contents of this file are subject to the MRPL 1.2
 * * (the  "License"),  being   the  Mozilla   Public  License
 * * Version 1.1  with a permitted attribution clause; you may not  use this
 * * file except in compliance with the License. You  may  obtain  a copy of
 * * the License at http://www.floreantpos.org/license.html
 * * Software distributed under the License  is  distributed  on  an "AS IS"
 * * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * * License for the specific  language  governing  rights  and  limitations
 * * under the License.
 * * The Original Code is FLOREANT POS.
 * * The Initial Developer of the Original Code is OROCUBE LLC
 * * All portions are Copyright (C) 2015 OROCUBE LLC
 * * All Rights Reserved.
 * ************************************************************************
 */
package com.floreantpos.bo.ui.explorer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXTable;

import com.floreantpos.POSConstants;
import com.floreantpos.bo.ui.BOMessageDialog;
import com.floreantpos.model.AttendenceHistory;
import com.floreantpos.model.User;
import com.floreantpos.model.dao.AttendenceHistoryDAO;
import com.floreantpos.model.dao.UserDAO;
import com.floreantpos.swing.ListTableModel;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.PosTableRenderer;
import com.floreantpos.ui.dialog.DateChoserDialog;
import com.floreantpos.ui.dialog.POSMessageDialog;
import com.floreantpos.ui.util.UiUtil;

public class AttendanceHistoryExplorer extends TransparentPanel {
	private SimpleDateFormat dateFormat = new SimpleDateFormat("MMMdd  HH:mm"); //$NON-NLS-1$
	private JXDatePicker fromDatePicker = UiUtil.getCurrentMonthStart();
	private JXDatePicker toDatePicker = UiUtil.getCurrentMonthEnd();
	private JButton btnGo = new JButton(com.floreantpos.POSConstants.GO);
	private JButton btnEdit = new JButton("Edit");
	private JButton btnPrint = new JButton("Print");
	private JXTable table;
	private JComboBox cbUserType;

	public AttendanceHistoryExplorer() {
		super(new BorderLayout());
		add(new JScrollPane(table = new JXTable(new AttendenceHistoryTableModel(AttendenceHistoryDAO.getInstance().findAll()))));
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Object.class, new PosTableRenderer());
		JPanel topPanel = new JPanel(new MigLayout());

		cbUserType = new JComboBox();

		UserDAO dao = new UserDAO();
		List<User> userTypes = dao.findAll();

		Vector list = new Vector();
		list.add(POSConstants.ALL);
		list.addAll(userTypes);

		cbUserType.setModel(new DefaultComboBoxModel(list));

		topPanel.add(new JLabel(com.floreantpos.POSConstants.START_DATE), "grow"); //$NON-NLS-1$
		topPanel.add(fromDatePicker); //$NON-NLS-1$
		topPanel.add(new JLabel(com.floreantpos.POSConstants.END_DATE), "grow"); //$NON-NLS-1$
		topPanel.add(toDatePicker); //$NON-NLS-1$
		topPanel.add(new JLabel(POSConstants.USER + ":")); //$NON-NLS-1$
		topPanel.add(cbUserType);
		topPanel.add(btnGo, "skip 1, al right"); //$NON-NLS-1$
		add(topPanel, BorderLayout.NORTH);

		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPanel.add(btnEdit);
		//bottomPanel.add(btnPrint);
		add(bottomPanel, BorderLayout.SOUTH);

		/*btnPrint.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedRow = table.getSelectedRow();
				if (selectedRow < 0) {
					BOMessageDialog.showError(AttendanceHistoryExplorer.this, "Please select a row to print");
					return;
				}
				AttendenceHistoryTableModel model = (AttendenceHistoryTableModel) table.getModel();
				AttendenceHistory report = (AttendenceHistory) model.getRowData(selectedRow);

				//PosPrintService.printDrawerPullReport(report, report.getTerminal());
			}
		});*/

		btnGo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
					viewReport();
					//resizeColumnWidth(table);
				} catch (Exception e1) {
					BOMessageDialog.showError(AttendanceHistoryExplorer.this, POSConstants.ERROR_MESSAGE, e1);
				}
			}

		});

		btnEdit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedRow = table.getSelectedRow();
				if (selectedRow < 0) {
					BOMessageDialog.showError(AttendanceHistoryExplorer.this, "Select a row to edit");
					return;
				}
				AttendenceHistoryTableModel model = (AttendenceHistoryTableModel) table.getModel();
				AttendenceHistory history = (AttendenceHistory) model.getRowData(selectedRow);

				DateChoserDialog dialog = new DateChoserDialog(history, "Edit clock in/out time");
				dialog.pack();
				dialog.open();

				if (dialog.isCanceled()) {
					return;
				}

				if (dialog.getAttendenceHistory() != null) {
					history = dialog.getAttendenceHistory();
				}

				Calendar clockInTime = dialog.getClockInCalendar();
				Calendar clockOutTime = dialog.getClockOutCalendar();

				history.setClockInTime(clockInTime.getTime());
				history.setClockInHour(Short.valueOf((short) clockInTime.get(Calendar.HOUR_OF_DAY)));

				history.setClockOutTime(clockOutTime.getTime());
				history.setClockOutHour(Short.valueOf((short) clockOutTime.get(Calendar.HOUR_OF_DAY)));

				AttendenceHistoryDAO dao = new AttendenceHistoryDAO();
				dao.saveOrUpdate(history);
				model.updateItem(selectedRow);
			}
		});
	}

	private void viewReport() {
		try {
			Date fromDate = fromDatePicker.getDate();
			Date toDate = toDatePicker.getDate();

			if (fromDate.after(toDate)) {
				POSMessageDialog.showError(com.floreantpos.util.POSUtil.getFocusedWindow(),
						com.floreantpos.POSConstants.FROM_DATE_CANNOT_BE_GREATER_THAN_TO_DATE_);
				return;
			}

			Calendar calendar = Calendar.getInstance();
			calendar.clear();

			Calendar calendar2 = Calendar.getInstance();
			calendar2.setTime(fromDate);

			calendar.set(Calendar.YEAR, calendar2.get(Calendar.YEAR));
			calendar.set(Calendar.MONTH, calendar2.get(Calendar.MONTH));
			calendar.set(Calendar.DATE, calendar2.get(Calendar.DATE));
			calendar.set(Calendar.HOUR, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			fromDate = calendar.getTime();

			calendar.clear();
			calendar2.setTime(toDate);
			calendar.set(Calendar.YEAR, calendar2.get(Calendar.YEAR));
			calendar.set(Calendar.MONTH, calendar2.get(Calendar.MONTH));
			calendar.set(Calendar.DATE, calendar2.get(Calendar.DATE));
			calendar.set(Calendar.HOUR, 23);
			calendar.set(Calendar.MINUTE, 59);
			calendar.set(Calendar.SECOND, 59);
			toDate = calendar.getTime();

			User user = null;
			if (!cbUserType.getSelectedItem().equals(POSConstants.ALL)) {
				user = (User) cbUserType.getSelectedItem();
			}

			AttendenceHistoryDAO dao = new AttendenceHistoryDAO();
			List<AttendenceHistory> historyList = dao.findHistory(fromDate, toDate, user);
			AttendenceHistoryTableModel model = (AttendenceHistoryTableModel) table.getModel();
			model.setRows(historyList);
		} catch (Exception e) {
			BOMessageDialog.showError(this, POSConstants.ERROR_MESSAGE, e);
		}
	}

	class AttendenceHistoryTableModel extends ListTableModel {
		String[] columnNames = { "CLOCK IN TIME", "CLOCK OUT TIME", "CLOCKED OUT", "EMP ID", "EMP NAME", "SHIFT ID", "TERMINAL ID" };/* "CLOCK IN HOUR", "CLOCK OUT HOUR", */

		AttendenceHistoryTableModel(List<AttendenceHistory> list) {
			setRows(list);
			setColumnNames(columnNames);
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			AttendenceHistory history = (AttendenceHistory) rows.get(rowIndex);

			switch (columnIndex) {

			/*	case 0:
				//	return history.getId().toString();
			*/
				case 0:

					Date date = history.getClockInTime();
					if (date != null) {
						return dateFormat.format(date);
					}
					return "";

				case 1:

					Date date2 = history.getClockOutTime();
					if (date2 != null) {
						return dateFormat.format(date2);
					}
					return "";

					/*	case 3:
						//	return history.getClockInHour();

						case 4:
						//	return history.getClockOutHour();
					*/
				case 2:
					return history.isClockedOut();

				case 3:
					return history.getUser().getAutoId();
					
				case 4:
					return history.getUser().getFirstName() + " " + history.getUser().getLastName();
					
				case 5:
					return history.getShift().getId();
					
				case 6:
					return history.getTerminal().getId();
			}
			return null;
		}
	}
}
