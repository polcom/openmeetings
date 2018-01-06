/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.common;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.panel.KendoFeedbackPanel;

public abstract class FormSaveRefreshPanel<T> extends Panel {
	private static final long serialVersionUID = 1L;
	private final Form<T> form;
	protected final KendoFeedbackPanel feedback = new KendoFeedbackPanel("feedback", new Options("button", true));

	public FormSaveRefreshPanel(String id, Form<T> form) {
		super(id);
		this.form = form;
		setOutputMarkupId(true);
	}

	@Override
	protected void onInitialize() {
		add(feedback.setOutputMarkupId(true));

		// add a save button that can be used to submit the form via ajax
		add(new AjaxButton("ajax-save-button", form) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				// repaint the feedback panel so that it is hidden
				target.add(feedback);
				onSaveSubmit(target, form);
			}

			@Override
			protected void onError(AjaxRequestTarget target) {
				// repaint the feedback panel so errors are shown
				target.add(feedback);
				onSaveError(target, form);
			}
		});

		// add a refresh button that can be used to submit the form via ajax
		add(new AjaxButton("ajax-refresh-button", form) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target) {
				// repaint the feedback panel so that it is hidden
				target.add(feedback);
				hideNewRecord();
				onRefreshSubmit(target, form);
			}

			@Override
			protected void onError(AjaxRequestTarget target) {
				// repaint the feedback panel so errors are shown
				target.add(feedback);
				hideNewRecord();
				onRefreshError(target, form);
			}
		});
		super.onInitialize();
	}

	/**
	 * Hide the new record text
	 */
	public void hideNewRecord() {
		// for admin only, will be implemented in admin
	}

	protected abstract void onSaveSubmit(AjaxRequestTarget target, Form<?> form);

	/**
	 * Save error handler
	 *
	 * @param target Ajax target
	 * @param form form object
	 */
	protected void onSaveError(AjaxRequestTarget target, Form<?> form) {
		//no-op
	}

	protected abstract void onRefreshSubmit(AjaxRequestTarget target, Form<?> form);


	/**
	 * Refresh error handler
	 *
	 * @param target Ajax target
	 * @param form form object
	 */
	protected void onRefreshError(AjaxRequestTarget target, Form<?> form) {
		//no-op
	}
}
