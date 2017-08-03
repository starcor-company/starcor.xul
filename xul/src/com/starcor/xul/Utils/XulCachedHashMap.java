package com.starcor.xul.Utils;


import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.*;

/**
 * Created by hy on 2015/5/29.
 * copy from android 19 source code
 */
public class XulCachedHashMap<K, V> extends AbstractMap<K, V> implements Cloneable, Serializable {
	private static final int MINIMUM_CAPACITY = 4;

	private static final int MAXIMUM_CAPACITY = 1 << 30;

	private static final Entry[] EMPTY_TABLE
		= new HashMapEntry[MINIMUM_CAPACITY >>> 1];

	static final float DEFAULT_LOAD_FACTOR = .75F;

	transient HashMapEntry<K, V>[] table;

	transient HashMapEntry<K, V> entryForNullKey;

	transient int size;

	transient int modCount;

	private transient int threshold;

	// Views - lazily initialized
	private transient Set<K> keySet;
	private transient Set<Entry<K, V>> entrySet;
	private transient Collection<V> values;

	@SuppressWarnings("unchecked")
	public XulCachedHashMap() {
		table = (HashMapEntry<K, V>[]) EMPTY_TABLE;
		threshold = -1; // Forces first put invocation to replace EMPTY_TABLE
	}

	public XulCachedHashMap(int capacity) {
		if (capacity < 0) {
			throw new IllegalArgumentException("Capacity: " + capacity);
		}

		if (capacity == 0) {
			@SuppressWarnings("unchecked")
			HashMapEntry<K, V>[] tab = (HashMapEntry<K, V>[]) EMPTY_TABLE;
			table = tab;
			threshold = -1; // Forces first put() to replace EMPTY_TABLE
			return;
		}

		if (capacity < MINIMUM_CAPACITY) {
			capacity = MINIMUM_CAPACITY;
		} else if (capacity > MAXIMUM_CAPACITY) {
			capacity = MAXIMUM_CAPACITY;
		} else {
			capacity = roundUpToPowerOfTwo(capacity);
		}
		makeTable(capacity);
	}

	public XulCachedHashMap(int capacity, float loadFactor) {
		this(capacity);

		if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
			throw new IllegalArgumentException("Load factor: " + loadFactor);
		}
	}

	public XulCachedHashMap(Map<? extends K, ? extends V> map) {
		this(capacityForInitSize(map.size()));
		constructorPutAll(map);
	}

	final void constructorPutAll(Map<? extends K, ? extends V> map) {
		if (table == EMPTY_TABLE) {
			doubleCapacity(); // Don't do unchecked puts to a shared table.
		}
		for (Entry<? extends K, ? extends V> e : map.entrySet()) {
			constructorPut(e.getKey(), e.getValue());
		}
	}

	static int capacityForInitSize(int size) {
		int result = (size >> 1) + size; // Multiply by 3/2 to allow for growth

		// boolean expr is equivalent to result >= 0 && result<MAXIMUM_CAPACITY
		return (result & ~(MAXIMUM_CAPACITY - 1)) == 0 ? result : MAXIMUM_CAPACITY;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object clone() {
	    /*
	     * This could be made more efficient. It unnecessarily hashes all of
         * the elements in the map.
         */
		XulCachedHashMap<K, V> result;
		try {
			result = (XulCachedHashMap<K, V>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}

		// Restore clone to empty state, retaining our capacity and threshold
		result.makeTable(table.length);
		result.entryForNullKey = null;
		result.size = 0;
		result.keySet = null;
		result.entrySet = null;
		result.values = null;

		result.init(); // Give subclass a chance to initialize itself
		result.constructorPutAll(this); // Calls method overridden in subclass!!
		return result;
	}

	void init() {
	}

	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	@Override
	public int size() {
		return size;
	}

	public V get(Object key) {
		if (key == null) {
			HashMapEntry<K, V> e = entryForNullKey;
			return e == null ? null : e.value;
		}

		// Doug Lea's supplemental secondaryHash function (inlined).
		// Replace with Collections.secondaryHash when the VM is fast enough (http://b/8290590).
		int hash = key.hashCode();
		hash ^= (hash >>> 20) ^ (hash >>> 12);
		hash ^= (hash >>> 7) ^ (hash >>> 4);

		HashMapEntry<K, V>[] tab = table;
		for (HashMapEntry<K, V> e = tab[hash & (tab.length - 1)];
		     e != null; e = e.next) {
			K eKey = e.key;
			if (eKey == key || (e.hash == hash && key.equals(eKey))) {
				return e.value;
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key == null) {
			return entryForNullKey != null;
		}

		// Doug Lea's supplemental secondaryHash function (inlined).
		// Replace with Collections.secondaryHash when the VM is fast enough (http://b/8290590).
		int hash = key.hashCode();
		hash ^= (hash >>> 20) ^ (hash >>> 12);
		hash ^= (hash >>> 7) ^ (hash >>> 4);

		HashMapEntry<K, V>[] tab = table;
		for (HashMapEntry<K, V> e = tab[hash & (tab.length - 1)];
		     e != null; e = e.next) {
			K eKey = e.key;
			if (eKey == key || (e.hash == hash && key.equals(eKey))) {
				return true;
			}
		}
		return false;
	}

	// Doug Lea's supplemental secondaryHash function (non-inlined).
	// Replace with Collections.secondaryHash when the VM is fast enough (http://b/8290590).
	static int secondaryHash(Object key) {
		int hash = key.hashCode();
		hash ^= (hash >>> 20) ^ (hash >>> 12);
		hash ^= (hash >>> 7) ^ (hash >>> 4);
		return hash;
	}

	@Override
	public boolean containsValue(Object value) {
		HashMapEntry[] tab = table;
		int len = tab.length;
		if (value == null) {
			for (int i = 0; i < len; i++) {
				for (HashMapEntry e = tab[i]; e != null; e = e.next) {
					if (e.value == null) {
						return true;
					}
				}
			}
			return entryForNullKey != null && entryForNullKey.value == null;
		}

		// value is non-null
		for (int i = 0; i < len; i++) {
			for (HashMapEntry e = tab[i]; e != null; e = e.next) {
				if (value.equals(e.value)) {
					return true;
				}
			}
		}
		return entryForNullKey != null && value.equals(entryForNullKey.value);
	}

	@Override
	public V put(K key, V value) {
		if (key == null) {
			return putValueForNullKey(value);
		}

		int hash = secondaryHash(key);
		HashMapEntry<K, V>[] tab = table;
		int index = hash & (tab.length - 1);
		for (HashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
			if (e.hash == hash && key.equals(e.key)) {
				preModify(e);
				V oldValue = e.value;
				e.value = value;
				return oldValue;
			}
		}

		// No entry for (non-null) key is present; create one
		modCount++;
		if (size++ > threshold) {
			tab = doubleCapacity();
			index = hash & (tab.length - 1);
		}
		addNewEntry(key, value, hash, index);
		return null;
	}

	private V putValueForNullKey(V value) {
		HashMapEntry<K, V> entry = entryForNullKey;
		if (entry == null) {
			addNewEntryForNullKey(value);
			size++;
			modCount++;
			return null;
		} else {
			preModify(entry);
			V oldValue = entry.value;
			entry.value = value;
			return oldValue;
		}
	}

	void preModify(HashMapEntry<K, V> e) {
	}

	private void constructorPut(K key, V value) {
		if (key == null) {
			HashMapEntry<K, V> entry = entryForNullKey;
			if (entry == null) {
				entryForNullKey = constructorNewEntry(null, value, 0, null);
				size++;
			} else {
				entry.value = value;
			}
			return;
		}

		int hash = secondaryHash(key);
		HashMapEntry<K, V>[] tab = table;
		int index = hash & (tab.length - 1);
		HashMapEntry<K, V> first = tab[index];
		for (HashMapEntry<K, V> e = first; e != null; e = e.next) {
			if (e.hash == hash && key.equals(e.key)) {
				e.value = value;
				return;
			}
		}

		// No entry for (non-null) key is present; create one
		tab[index] = constructorNewEntry(key, value, hash, first);
		size++;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		ensureCapacity(map.size());
		super.putAll(map);
	}

	private void ensureCapacity(int numMappings) {
		int newCapacity = roundUpToPowerOfTwo(capacityForInitSize(numMappings));
		HashMapEntry<K, V>[] oldTable = table;
		int oldCapacity = oldTable.length;
		if (newCapacity <= oldCapacity) {
			return;
		}
		if (newCapacity == oldCapacity * 2) {
			doubleCapacity();
			return;
		}

		// We're growing by at least 4x, rehash in the obvious way
		HashMapEntry<K, V>[] newTable = makeTable(newCapacity);
		if (size != 0) {
			int newMask = newCapacity - 1;
			for (int i = 0; i < oldCapacity; i++) {
				for (HashMapEntry<K, V> e = oldTable[i]; e != null; ) {
					HashMapEntry<K, V> oldNext = e.next;
					int newIndex = e.hash & newMask;
					HashMapEntry<K, V> newNext = newTable[newIndex];
					newTable[newIndex] = e;
					e.next = newNext;
					e = oldNext;
				}
			}
		}
	}

	private HashMapEntry<K, V>[] makeTable(int newCapacity) {
		@SuppressWarnings("unchecked") HashMapEntry<K, V>[] newTable
			= (HashMapEntry<K, V>[]) new HashMapEntry[newCapacity];
		table = newTable;
		threshold = (newCapacity >> 1) + (newCapacity >> 2); // 3/4 capacity
		return newTable;
	}

	private HashMapEntry<K, V>[] doubleCapacity() {
		HashMapEntry<K, V>[] oldTable = table;
		int oldCapacity = oldTable.length;
		if (oldCapacity == MAXIMUM_CAPACITY) {
			return oldTable;
		}
		int newCapacity = oldCapacity * 2;
		HashMapEntry<K, V>[] newTable = makeTable(newCapacity);
		if (size == 0) {
			return newTable;
		}

		for (int j = 0; j < oldCapacity; j++) {
		    /*
		     * Rehash the bucket using the minimum number of field writes.
             * This is the most subtle and delicate code in the class.
             */
			HashMapEntry<K, V> e = oldTable[j];
			if (e == null) {
				continue;
			}
			int highBit = e.hash & oldCapacity;
			HashMapEntry<K, V> broken = null;
			newTable[j | highBit] = e;
			for (HashMapEntry<K, V> n = e.next; n != null; e = n, n = n.next) {
				int nextHighBit = n.hash & oldCapacity;
				if (nextHighBit != highBit) {
					if (broken == null)
						newTable[j | nextHighBit] = n;
					else
						broken.next = n;
					broken = e;
					highBit = nextHighBit;
				}
			}
			if (broken != null)
				broken.next = null;
		}
		return newTable;
	}

	@Override
	public V remove(Object key) {
		if (key == null) {
			return removeNullKey();
		}
		int hash = secondaryHash(key);
		HashMapEntry<K, V>[] tab = table;
		int index = hash & (tab.length - 1);
		for (HashMapEntry<K, V> e = tab[index], prev = null;
		     e != null; prev = e, e = e.next) {
			if (e.hash == hash && key.equals(e.key)) {
				if (prev == null) {
					tab[index] = e.next;
				} else {
					prev.next = e.next;
				}
				modCount++;
				size--;
				V value = e.value;
				postRemove(e);
				return value;
			}
		}
		return null;
	}

	private V removeNullKey() {
		HashMapEntry<K, V> e = entryForNullKey;
		if (e == null) {
			return null;
		}
		entryForNullKey = null;
		modCount++;
		size--;
		V value = e.value;
		postRemove(e);
		return value;
	}

	@Override
	public void clear() {
		if (size != 0) {
			recycleTable(table);
			Arrays.fill(table, null);
			HashMapEntry<K, V> e = entryForNullKey;
			entryForNullKey = null;
			if (e != null) {
				postRemove(e);
			}
			modCount++;
			size = 0;
		}
	}

	@Override
	public Set<K> keySet() {
		Set<K> ks = keySet;
		return (ks != null) ? ks : (keySet = new KeySet());
	}

	@Override
	public Collection<V> values() {
		Collection<V> vs = values;
		return (vs != null) ? vs : (values = new Values());
	}

	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> es = entrySet;
		return (es != null) ? es : (entrySet = new EntrySet());
	}

	static class HashMapEntry<K, V> implements Entry<K, V> {
		K key;
		V value;
		int hash;
		HashMapEntry<K, V> next;

		HashMapEntry(K key, V value, int hash, HashMapEntry<K, V> next) {
			update(key, value, hash, next);
		}

		HashMapEntry<K, V> update(K key, V value, int hash, HashMapEntry<K, V> next) {
			this.key = key;
			this.value = value;
			this.hash = hash;
			this.next = next;
			return this;
		}

		public final K getKey() {
			return key;
		}

		public final V getValue() {
			return value;
		}

		public final V setValue(V value) {
			V oldValue = this.value;
			this.value = value;
			return oldValue;
		}

		@Override
		public final boolean equals(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry<?, ?> e = (Entry<?, ?>) o;
			return testEquals(e.getKey(), key)
				&& testEquals(e.getValue(), value);
		}

		@Override
		public final int hashCode() {
			return (key == null ? 0 : key.hashCode()) ^
				(value == null ? 0 : value.hashCode());
		}

		@Override
		public final String toString() {
			return key + "=" + value;
		}
	}

	private abstract class HashIterator {
		int nextIndex;
		HashMapEntry<K, V> nextEntry = entryForNullKey;
		HashMapEntry<K, V> lastEntryReturned;
		int expectedModCount = modCount;

		HashIterator() {
			if (nextEntry == null) {
				HashMapEntry<K, V>[] tab = table;
				HashMapEntry<K, V> next = null;
				while (next == null && nextIndex < tab.length) {
					next = tab[nextIndex++];
				}
				nextEntry = next;
			}
		}

		public boolean hasNext() {
			return nextEntry != null;
		}

		HashMapEntry<K, V> nextEntry() {
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			if (nextEntry == null)
				throw new NoSuchElementException();

			HashMapEntry<K, V> entryToReturn = nextEntry;
			HashMapEntry<K, V>[] tab = table;
			HashMapEntry<K, V> next = entryToReturn.next;
			while (next == null && nextIndex < tab.length) {
				next = tab[nextIndex++];
			}
			nextEntry = next;
			return lastEntryReturned = entryToReturn;
		}

		public void remove() {
			if (lastEntryReturned == null)
				throw new IllegalStateException();
			if (modCount != expectedModCount)
				throw new ConcurrentModificationException();
			XulCachedHashMap.this.remove(lastEntryReturned.key);
			lastEntryReturned = null;
			expectedModCount = modCount;
		}
	}

	private final class KeyIterator extends HashIterator
		implements Iterator<K> {
		public K next() {
			return nextEntry().key;
		}
	}

	private final class ValueIterator extends HashIterator
		implements Iterator<V> {
		public V next() {
			return nextEntry().value;
		}
	}

	private final class EntryIterator extends HashIterator
		implements Iterator<Entry<K, V>> {
		public Entry<K, V> next() {
			return nextEntry();
		}
	}

	private boolean containsMapping(Object key, Object value) {
		if (key == null) {
			HashMapEntry<K, V> e = entryForNullKey;
			return e != null && testEquals(value, e.value);
		}

		int hash = secondaryHash(key);
		HashMapEntry<K, V>[] tab = table;
		int index = hash & (tab.length - 1);
		for (HashMapEntry<K, V> e = tab[index]; e != null; e = e.next) {
			if (e.hash == hash && key.equals(e.key)) {
				return testEquals(value, e.value);
			}
		}
		return false; // No entry for key
	}

	private boolean removeMapping(Object key, Object value) {
		if (key == null) {
			HashMapEntry<K, V> e = entryForNullKey;
			if (e == null || !testEquals(value, e.value)) {
				return false;
			}
			entryForNullKey = null;
			modCount++;
			size--;
			postRemove(e);
			return true;
		}

		int hash = secondaryHash(key);
		HashMapEntry<K, V>[] tab = table;
		int index = hash & (tab.length - 1);
		for (HashMapEntry<K, V> e = tab[index], prev = null;
		     e != null; prev = e, e = e.next) {
			if (e.hash == hash && key.equals(e.key)) {
				if (!testEquals(value, e.value)) {
					return false;  // Map has wrong value for key
				}
				if (prev == null) {
					tab[index] = e.next;
				} else {
					prev.next = e.next;
				}
				modCount++;
				size--;
				postRemove(e);
				return true;
			}
		}
		return false; // No entry for key
	}

	// Subclass (LinkedHashMap) overrides these for correct iteration order
	Iterator<K> newKeyIterator() {
		return new KeyIterator();
	}

	Iterator<V> newValueIterator() {
		return new ValueIterator();
	}

	Iterator<Entry<K, V>> newEntryIterator() {
		return new EntryIterator();
	}

	private final class KeySet extends AbstractSet<K> {
		public Iterator<K> iterator() {
			return newKeyIterator();
		}

		public int size() {
			return size;
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public boolean contains(Object o) {
			return containsKey(o);
		}

		public boolean remove(Object o) {
			int oldSize = size;
			XulCachedHashMap.this.remove(o);
			return size != oldSize;
		}

		public void clear() {
			XulCachedHashMap.this.clear();
		}
	}

	private final class Values extends AbstractCollection<V> {
		public Iterator<V> iterator() {
			return newValueIterator();
		}

		public int size() {
			return size;
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public boolean contains(Object o) {
			return containsValue(o);
		}

		public void clear() {
			XulCachedHashMap.this.clear();
		}
	}

	private final class EntrySet extends AbstractSet<Entry<K, V>> {
		public Iterator<Entry<K, V>> iterator() {
			return newEntryIterator();
		}

		public boolean contains(Object o) {
			if (!(o instanceof Entry))
				return false;
			Entry<?, ?> e = (Entry<?, ?>) o;
			return containsMapping(e.getKey(), e.getValue());
		}

		public boolean remove(Object o) {
			if (!(o instanceof Entry))
				return false;
			Entry<?, ?> e = (Entry<?, ?>) o;
			return removeMapping(e.getKey(), e.getValue());
		}

		public int size() {
			return size;
		}

		public boolean isEmpty() {
			return size == 0;
		}

		public void clear() {
			XulCachedHashMap.this.clear();
		}
	}

	private static final long serialVersionUID = 362498820763181265L;

	private static final ObjectStreamField[] serialPersistentFields = {
		new ObjectStreamField("loadFactor", float.class)
	};

	private void writeObject(ObjectOutputStream stream) throws IOException {
		// Emulate loadFactor field for other implementations to read
		ObjectOutputStream.PutField fields = stream.putFields();
		fields.put("loadFactor", DEFAULT_LOAD_FACTOR);
		stream.writeFields();

		stream.writeInt(table.length); // Capacity
		stream.writeInt(size);
		for (Entry<K, V> e : entrySet()) {
			stream.writeObject(e.getKey());
			stream.writeObject(e.getValue());
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException,
		ClassNotFoundException {
		stream.defaultReadObject();
		int capacity = stream.readInt();
		if (capacity < 0) {
			throw new InvalidObjectException("Capacity: " + capacity);
		}
		if (capacity < MINIMUM_CAPACITY) {
			capacity = MINIMUM_CAPACITY;
		} else if (capacity > MAXIMUM_CAPACITY) {
			capacity = MAXIMUM_CAPACITY;
		} else {
			capacity = roundUpToPowerOfTwo(capacity);
		}
		makeTable(capacity);

		int size = stream.readInt();
		if (size < 0) {
			throw new InvalidObjectException("Size: " + size);
		}

		init(); // Give subclass (LinkedHashMap) a chance to initialize itself
		for (int i = 0; i < size; i++) {
			@SuppressWarnings("unchecked") K key = (K) stream.readObject();
			@SuppressWarnings("unchecked") V val = (V) stream.readObject();
			constructorPut(key, val);
		}
	}

	private static int roundUpToPowerOfTwo(int v) {
		int newVal = 1;
		while (v > 1) {
			int rm = 2;
			if (v >= 0x1000) {
				rm = 0x1000;
			} else if (v >= 0x10) {
				rm = 0x10;
			}
			newVal *= rm;
			if (v % rm > 0) {
				v /= rm;
				++v;
			} else {
				v /= rm;
			}
		}
		return newVal;
	}

	private static <K, V> boolean testEquals(K v1, V v2) {
		return v1 == v2 || (v1 != null && v1.equals(v2));
	}


	void addNewEntry(K key, V value, int hash, int index) {
		table[index] = constructorNewEntry(key, value, hash, table[index]);
	}

	void addNewEntryForNullKey(V value) {
		entryForNullKey = constructorNewEntry(null, value, 0, null);
	}

	static final ArrayList<HashMapEntry> _cachedEntries = new ArrayList<HashMapEntry>(4096);

	static <K, V> HashMapEntry<K, V> constructorNewEntry(K key, V value, int hash, HashMapEntry<K, V> first) {
		synchronized (_cachedEntries) {
			if (_cachedEntries.isEmpty()) {
				return new HashMapEntry<K, V>(key, value, hash, first);
			} else {
				HashMapEntry entry;
				int index = _cachedEntries.size() - 1;
				entry = _cachedEntries.remove(index);
				if (entry.next != null) {
					_cachedEntries.add(entry.next);
				}
				return entry.update(key, value, hash, first);
			}
		}
	}

	static <K, V> void postRemove(HashMapEntry<K, V> e) {
		e.next = null;
		synchronized (_cachedEntries) {
			recycleEntry(e);
		}
	}

	private static <K, V> void recycleEntry(HashMapEntry<K, V> e) {
		e.key = null;
		e.value = null;
		_cachedEntries.add(e);
	}

	static <K, V> void recycleTable(HashMapEntry<K, V>[] tab) {
		synchronized (_cachedEntries) {
			for (int i = 0, tabLength = tab.length; i < tabLength; i++) {
				HashMapEntry<K, V> entry = tab[i];
				if (entry != null) {
					recycleEntry(entry);
				}
			}
		}
	}
}
