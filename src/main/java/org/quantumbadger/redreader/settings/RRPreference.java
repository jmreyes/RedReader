/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.settings;

import android.content.Context;
import android.net.Uri;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.WeakReferenceListManager;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class RRPreference {

	private final WeakReferenceListManager<Listener> listeners = new WeakReferenceListManager<Listener>();
	private final ListenerOperator listenerNotifyOperator = new ListenerOperator(this);

	private final RRPrefs preferenceManager;

	public final String id;
	public final int titleString;

	private ItemSource itemSource;

	public Uri getUri() {
		return Constants.Internal.getUri(Constants.Internal.URI_HOST_PREF, id);
	}

	protected static RRPreference parse(RRPrefs preferenceManager, XmlParserWrapper parser) throws XmlParserWrapper.RRParseException, NoSuchFieldException, IllegalAccessException, IOException, XmlPullParserException {

		try {

			final HashMap<String, String> attributes = parser.getAttributeMap();

			final ItemSource itemSource;

			if(attributes.containsKey("items")) {
				itemSource = new ReferenceItemSource(attributes.get("items"));
			} else {

				final LinkedList<Item> items = new LinkedList<Item>();

				while(parser.next() != XmlPullParser.END_TAG) {

					if(parser.getEventType() != XmlPullParser.START_TAG) throw new RuntimeException("Expecting start tag for item, got " + parser.getEventType());

					final HashMap<String, String> itemAttributes = parser.getAttributeMap();

					final String itemValue = itemAttributes.get("value");

					if(itemAttributes.containsKey("str")) {
						items.add(new LocaleItem(itemValue, getStringByKey(itemAttributes.get("str"))));
					} else if(itemAttributes.containsKey("lstr")) {
						items.add(new LiteralItem(itemValue, itemAttributes.get("lstr")));
					} else {
						throw new RuntimeException("Item without a title string");
					}

					if(parser.next() != XmlPullParser.END_TAG) throw new RuntimeException("Expecting end tag for item");
				}

				itemSource = new ArrayItemSource(items.toArray(new Item[items.size()]));
			}

			final String type = parser.getName();

			if(type.equals("Float")) {
				return RRPreferenceFloat.parse(preferenceManager, attributes, itemSource);
			} else if(type.equals("Boolean")) {
				return RRPreferenceBoolean.parse(preferenceManager, attributes, itemSource);
			} else if(type.equals("Enum")) {
				return RRPreferenceEnum.parse(preferenceManager, attributes, itemSource);
			} else if(type.equals("Header")) {
				return RRPreferenceHeader.parse(preferenceManager, attributes, itemSource);
			} else if(type.equals("Link")) {
				return RRPreferenceLink.parse(preferenceManager, attributes, itemSource);
			} else {
				throw new RuntimeException("Unknown preference type: " + type);
			}

		} catch(Throwable t) {
			throw parser.newException(t);
		}
	}

	protected RRPreference(RRPrefs preferenceManager, HashMap<String, String> attributes, ItemSource itemSource) throws NoSuchFieldException, IllegalAccessException {

		this.preferenceManager = preferenceManager;
		this.id = attributes.get("id");

		if(attributes.containsKey("str")) {
			titleString = getStringByKey(attributes.get("str"));
		} else if(id != null) {
			titleString = getStringByKey(id + "_title");
		} else {
			titleString = getStringByKey("prefs_error_invalidtitle");
		}

		this.itemSource = itemSource;
	}

	private static int getStringByKey(String key) throws NoSuchFieldException, IllegalAccessException {
		return R.string.class.getField(key).getInt(null);
	}

	protected void setRawUserPreference(String value) {
		preferenceManager.setRawUserPreference(id, value);
		listeners.map(listenerNotifyOperator);
	}

	protected RRPreference getPreferenceById(String name) throws NoSuchFieldException, IllegalAccessException {
		return preferenceManager.getPreferenceByName(name);
	}

	public final synchronized void addListener(Listener listener) {
		listeners.add(listener);
	}

	public final synchronized void removeListener(final Listener listener) {
		listeners.remove(listener);
	}

	public static interface Listener {
		public void onPreferenceChanged(RRPreference preference);
	}

	public static final class ListenerOperator implements WeakReferenceListManager.Operator<Listener> {

		private final RRPreference preference;

		public ListenerOperator(RRPreference preference) {
			this.preference = preference;
		}

		public void operate(Listener listener) {
			listener.onPreferenceChanged(preference);
		}
	}

	private ItemSource getItemSource() {
		return itemSource;
	}

	private void setItemSource(ItemSource itemSource) {
		this.itemSource = itemSource;
	}

	public Item[] getItems() {
		try {
			return itemSource.getItems(this);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}


	protected static abstract class ItemSource {
		public abstract Item[] getItems(RRPreference pref) throws NoSuchFieldException, IllegalAccessException;
	}

	protected static final class ArrayItemSource extends ItemSource {

		private final Item[] items;

		private ArrayItemSource(Item[] items) {
			this.items = items;
		}

		@Override
		public Item[] getItems(RRPreference pref) throws NoSuchFieldException, IllegalAccessException {
			return items;
		}
	}

	protected static final class ReferenceItemSource extends ItemSource {
		private final String itemSourceId;

		private ReferenceItemSource(String itemSourceId) {
			this.itemSourceId = itemSourceId;
		}

		@Override
		public Item[] getItems(RRPreference pref) throws NoSuchFieldException, IllegalAccessException {
			final RRPreference itemSourcePref = pref.getPreferenceById(itemSourceId);
			pref.setItemSource(itemSourcePref.getItemSource());
			return itemSourcePref.getItemSource().getItems(pref);
		}
	}

	public static abstract class Item {

		public final String value;

		protected Item(String value) {
			this.value = value;
		}

		public abstract String getName(Context context);
	}

	protected static final class LiteralItem extends Item {

		private final String name;

		private LiteralItem(String value, String name) {
			super(value);
			this.name = name;
		}

		@Override
		public String getName(Context context) {
			return name;
		}
	}

	protected static final class LocaleItem extends Item {

		private final int name;

		private LocaleItem(String value, int name) {
			super(value);
			this.name = name;
		}

		@Override
		public String getName(Context context) {
			return context.getString(name);
		}
	}

	public Item getItem(String value) {

		final Item[] allItems;

		try {
			allItems = getItems();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

		for(final Item item : allItems) {
			if(value.equals(item.value)) return item;
		}

		return null;
	}

	public boolean isGreyedOut() {
		return false;
	}
}
