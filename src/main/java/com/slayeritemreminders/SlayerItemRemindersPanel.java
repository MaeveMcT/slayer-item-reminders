package com.slayeritemreminders;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

@Singleton
final class SlayerItemRemindersPanel extends PluginPanel
{
	private static final String CONTENT_CARD = "content";
	private static final String EMPTY_CARD = "empty";
	private static final String AUTOMATIC = "Automatic (task default)";
	private static final HttpUrl WIKI_BASE = HttpUrl.get("https://oldschool.runescape.wiki");

	private final JLabel taskLabel = new JShadowedLabel();
	private final JLabel selectedVariantLabel = new JShadowedLabel();
	private final JLabel wikiLink = new JShadowedLabel("<html><u>View on OSRS Wiki</u></html>");
	private final JLabel statusLabel = new JShadowedLabel();
	private final IconTextField variantSearch = new IconTextField();
	private final JPanel variantRows = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
	private final JPanel display = new JPanel(new CardLayout());

	private BiConsumer<String, TaskVariant> selectionHandler = (task, variant) -> { };
	private List<TaskVariant> displayedVariants = new ArrayList<>();
	private TaskVariant selectedVariant;
	private String displayedTaskName;
	private String selectedWikiPage;
	private JPanel taskCard;
	private boolean rebuilding;

	@Inject
	SlayerItemRemindersPanel()
	{
		super(false);
		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);
		add(buildDisplay(), BorderLayout.CENTER);
		showTask(null, new ArrayList<>(), null, VariantLoadState.LOADED);
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		header.setBorder(new EmptyBorder(8, 8, 8, 8));

		JLabel title = new JShadowedLabel("Slayer Item Reminders");
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);
		header.add(title, BorderLayout.CENTER);
		return header;
	}

	private JPanel buildDisplay()
	{
		display.setBackground(ColorScheme.DARK_GRAY_COLOR);
		display.add(buildContent(), CONTENT_CARD);

		PluginErrorPanel emptyPanel = new PluginErrorPanel();
		emptyPanel.setContent("No active Slayer task", "Your current assignment will appear here.");
		display.add(emptyPanel, EMPTY_CARD);
		return display;
	}

	private JPanel buildContent()
	{
		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		taskCard = createCard();
		taskCard.setLayout(new BoxLayout(taskCard, BoxLayout.Y_AXIS));
		taskCard.add(createCaption("CURRENT TASK"));
		taskCard.add(Box.createVerticalStrut(5));
		taskLabel.setFont(FontManager.getRunescapeBoldFont());
		taskLabel.setForeground(Color.WHITE);
		taskLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		taskCard.add(taskLabel);
		content.add(taskCard, BorderLayout.NORTH);

		JPanel variantCard = createCard();
		variantCard.setLayout(new BorderLayout(0, 8));
		variantCard.add(buildVariantControls(), BorderLayout.NORTH);
		variantCard.add(buildVariantScroller(), BorderLayout.CENTER);
		content.add(variantCard, BorderLayout.CENTER);
		return content;
	}

	private JPanel buildVariantControls()
	{
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
		controls.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		controls.add(createCaption("SELECTED VARIANT"));
		controls.add(Box.createVerticalStrut(4));
		selectedVariantLabel.setFont(FontManager.getRunescapeBoldFont());
		selectedVariantLabel.setForeground(Color.WHITE);
		selectedVariantLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controls.add(selectedVariantLabel);
		controls.add(Box.createVerticalStrut(5));

		wikiLink.setFont(FontManager.getRunescapeSmallFont());
		wikiLink.setForeground(ColorScheme.BRAND_ORANGE);
		wikiLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		wikiLink.setAlignmentX(Component.LEFT_ALIGNMENT);
		wikiLink.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				wikiLink.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				wikiLink.setForeground(ColorScheme.BRAND_ORANGE);
			}

			@Override
			public void mousePressed(MouseEvent event)
			{
				openSelectedVariantWikiPage();
			}
		});
		controls.add(wikiLink);
		controls.add(Box.createVerticalStrut(10));
		controls.add(createCaption("FILTER VARIANTS"));
		controls.add(Box.createVerticalStrut(6));

		variantSearch.setIcon(IconTextField.Icon.SEARCH);
		variantSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
		variantSearch.setPreferredSize(new Dimension(190, 30));
		variantSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
		variantSearch.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				filterChanged();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				filterChanged();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				filterChanged();
			}
		});
		controls.add(variantSearch);
		controls.add(Box.createVerticalStrut(7));

		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		controls.add(statusLabel);
		return controls;
	}

	private JScrollPane buildVariantScroller()
	{
		variantRows.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JPanel northWrapper = new JPanel(new BorderLayout());
		northWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		northWrapper.add(variantRows, BorderLayout.NORTH);

		JScrollPane scroller = new JScrollPane(northWrapper);
		scroller.setBorder(null);
		scroller.setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroller.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
		return scroller;
	}

	private static JPanel createCard()
	{
		JPanel card = new JPanel();
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(ColorScheme.BORDER_COLOR),
			new EmptyBorder(10, 10, 10, 10)));
		return card;
	}

	private static JLabel createCaption(String text)
	{
		JLabel caption = new JShadowedLabel(text);
		caption.setFont(FontManager.getRunescapeSmallFont());
		caption.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		caption.setAlignmentX(Component.LEFT_ALIGNMENT);
		return caption;
	}

	void setSelectionHandler(BiConsumer<String, TaskVariant> selectionHandler)
	{
		this.selectionHandler = selectionHandler;
	}

	void showTask(String taskName, List<TaskVariant> variants, TaskVariant selected,
		VariantLoadState loadState)
	{
		List<TaskVariant> variantSnapshot = new ArrayList<>(variants);
		SwingUtilities.invokeLater(() -> rebuild(taskName, variantSnapshot, selected, loadState));
	}

	private void rebuild(String taskName, List<TaskVariant> variants, TaskVariant selected,
		VariantLoadState loadState)
	{
		rebuilding = true;
		try
		{
			displayedTaskName = taskName;
			displayedVariants = variants;
			selectedVariant = selected;
			CardLayout cards = (CardLayout) display.getLayout();
			cards.show(display, taskName == null ? EMPTY_CARD : CONTENT_CARD);
			if (taskName == null)
			{
				return;
			}

			taskLabel.setText("<html><body style='width:175px'>" + escapeHtml(taskName) + "</body></html>");
			taskLabel.setToolTipText(taskName);
			taskCard.setPreferredSize(new Dimension(taskCard.getPreferredSize().width,
				taskLabel.getPreferredSize().height + 38));

			String selectedName = selected == null ? AUTOMATIC : selected.getName();
			selectedVariantLabel.setText(selectedName);
			selectedVariantLabel.setToolTipText(selectedName);
			TaskVariant linkedVariant = selected != null ? selected
				: variants.isEmpty() ? null : variants.get(0);
			selectedWikiPage = linkedVariant == null ? null : linkedVariant.getWikiPage();
			wikiLink.setVisible(selectedWikiPage != null);
			wikiLink.setToolTipText(selectedWikiPage == null ? null : "Open " + selectedWikiPage + " on the OSRS Wiki");
			variantSearch.setText("");
			variantSearch.setEditable(!variants.isEmpty());
			if (loadState == VariantLoadState.LOADING)
			{
				statusLabel.setText("Loading Wiki variants…");
				statusLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
			}
			else if (loadState == VariantLoadState.UNAVAILABLE)
			{
				statusLabel.setText("Wiki variants unavailable; showing fallback");
				statusLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			}
			else
			{
				statusLabel.setText(variants.size() + " variant"
					+ (variants.size() == 1 ? "" : "s") + " available");
				statusLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			}
			rebuildVariantRows("");
			display.revalidate();
		}
		finally
		{
			rebuilding = false;
		}
	}

	private void filterChanged()
	{
		if (!rebuilding)
		{
			rebuildVariantRows(variantSearch.getText());
		}
	}

	private void rebuildVariantRows(String query)
	{
		String normalized = query.trim().toLowerCase(Locale.ENGLISH);
		variantRows.removeAll();
		int matches = 0;
		if (AUTOMATIC.toLowerCase(Locale.ENGLISH).contains(normalized))
		{
			variantRows.add(new VariantRow(AUTOMATIC, null, selectedVariant == null));
			matches++;
		}
		for (TaskVariant variant : displayedVariants)
		{
			if (variant.getName().toLowerCase(Locale.ENGLISH).contains(normalized))
			{
				variantRows.add(new VariantRow(variant.getName(), variant, isSelected(variant)));
				matches++;
			}
		}
		if (matches == 0)
		{
			JLabel noMatches = new JShadowedLabel("No matching variants");
			noMatches.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			noMatches.setHorizontalAlignment(SwingConstants.CENTER);
			noMatches.setBorder(new EmptyBorder(12, 4, 12, 4));
			variantRows.add(noMatches);
		}
		variantRows.revalidate();
		variantRows.repaint();
	}

	private void openSelectedVariantWikiPage()
	{
		if (selectedWikiPage == null)
		{
			return;
		}
		String url = WIKI_BASE.newBuilder()
			.addPathSegment("w")
			.addPathSegment(selectedWikiPage)
			.build()
			.toString();
		LinkBrowser.browse(url);
	}

	private boolean isSelected(TaskVariant variant)
	{
		return selectedVariant != null
			&& selectedVariant.getWikiPage().equalsIgnoreCase(variant.getWikiPage());
	}

	private static String escapeHtml(String text)
	{
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private final class VariantRow extends JPanel
	{
		private final boolean selected;

		private VariantRow(String name, TaskVariant variant, boolean selected)
		{
			this.selected = selected;
			setLayout(new BorderLayout(6, 0));
			setBackground(selected ? ColorScheme.DARKER_GRAY_HOVER_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			setBorder(BorderFactory.createCompoundBorder(
				new MatteBorder(0, 3, 0, 0, selected ? ColorScheme.BRAND_ORANGE : ColorScheme.BORDER_COLOR),
				new EmptyBorder(8, 8, 8, 8)));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

			JLabel nameLabel = new JShadowedLabel(name);
			nameLabel.setForeground(Color.WHITE);
			nameLabel.setFont(FontManager.getRunescapeSmallFont());
			add(nameLabel, BorderLayout.CENTER);

			if (selected)
			{
				JLabel selectedLabel = new JShadowedLabel("SELECTED");
				selectedLabel.setForeground(ColorScheme.BRAND_ORANGE);
				selectedLabel.setFont(FontManager.getRunescapeSmallFont());
				add(selectedLabel, BorderLayout.EAST);
			}

			MouseAdapter listener = new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent event)
				{
					if (!VariantRow.this.selected)
					{
						setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
					}
				}

				@Override
				public void mouseExited(MouseEvent event)
				{
					if (!VariantRow.this.selected)
					{
						setBackground(ColorScheme.DARKER_GRAY_COLOR);
					}
				}

				@Override
				public void mousePressed(MouseEvent event)
				{
					selectionHandler.accept(displayedTaskName, variant);
				}
			};
			addMouseListener(listener);
			nameLabel.addMouseListener(listener);
		}
	}
}
