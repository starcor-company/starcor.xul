package com.starcor.xul.PropMap;

import com.starcor.xul.Prop.XulProp;
import com.starcor.xul.Prop.XulPropNameCache;
import com.starcor.xul.Utils.XulSparseArray;

/**
 * Created by hy on 2014/5/5.
 */
class XulPriorityPropMap<T extends XulProp> extends XulSparseArray<XulPriorityPropMap.MapEntry<T>> implements IXulPropChain<T> {

	public void add(String key, T v, int state) {
		add(XulPropNameCache.name2Id(key), v, state);
	}

	public void add(int key, T v, int state) {
		MapEntry<T> headNode = super.get(key);
		if (headNode == null) {
			super.put(key, MapEntry.create(v, state));
			return;
		}

		final int newNodePriority = v.getPriority();
		MapEntry<T> entryNode = headNode;
		MapEntry<T> prevNode = null;
		while (entryNode != null) {
			T prop = entryNode.prop;
			if (prop == v) {
				// 已经存在相同的属性
				return;
			}
			if (newNodePriority > prop.getPriority()) {
				final MapEntry<T> newNode = MapEntry.create(v, state);
				if (prevNode == null) {
					newNode.next = entryNode;
					super.put(key, newNode);
				} else {
					prevNode.addNext(newNode);
				}
				return;
			}
			prevNode = entryNode;
			entryNode = entryNode.next;
		}
		prevNode.next = MapEntry.create(v, state);
	}

	public void add(String key, T v) {
		add(key, v, 0);
	}

	public void add(int key, T v) {
		add(key, v, 0);
	}

	@Override
	public T getVal(int key, int state) {
		MapEntry<T> headNode = super.get(key);
		if (headNode == null) {
			return null;
		}
		MapEntry<T> entryNode = headNode;
		while (entryNode != null) {
			if (entryNode.state == 0 || entryNode.state == state) {
				return entryNode.prop;
			}
			entryNode = entryNode.next;
		}
		return null;
	}

	@Override
	public T getValEx(int key, int state, int state2) {
	MapEntry<T> headNode = super.get(key);
		if (headNode == null) {
			return null;
		}
		MapEntry<T> entryNode = headNode;
		while (entryNode != null) {
			if (entryNode.state == 0 || entryNode.state == state || entryNode.state == state2) {
				return entryNode.prop;
			}
			entryNode = entryNode.next;
		}
		return null;
	}

	public <TIter extends XulProp> void each(IXulPropIterator<TIter> iterator) {
		for (int idx = super.nextIdx(-1); idx >= 0; idx = super.nextIdx(idx)) {
			MapEntry<T> headNode = super.get(idx);
			if (headNode == null) {
				continue;
			}
			MapEntry<T> entryNode = headNode;
			while (entryNode != null) {
				iterator.onProp((TIter) entryNode.prop, entryNode.state);
				entryNode = entryNode.next;
			}
		}
	}

	public void remove(T target) {
		int key = target.getNameId();

		MapEntry<T> headNode = super.get(key);
		if (headNode == null) {
			return;
		}
		MapEntry<T> entryNode = headNode;
		MapEntry<T> prevNode = null;
		while (entryNode != null) {
			if (target == entryNode.prop) {
				if (entryNode == headNode) {
					super.put(key, entryNode.next);
				} else {
					prevNode.removeNext();
				}
				break;
			}
			prevNode = entryNode;
			entryNode = entryNode.next;
		}
	}

	public void remove(T target, int state) {
		int key = target.getNameId();

		MapEntry<T> headNode = super.get(key);
		if (headNode == null) {
			return;
		}
		MapEntry<T> entryNode = headNode;
		MapEntry<T> prevNode = null;
		while (entryNode != null) {
			if (target == entryNode.prop && state == entryNode.state) {
				if (entryNode == headNode) {
					super.put(key, entryNode.next);
				} else {
					prevNode.removeNext();
				}
				break;
			}
			prevNode = entryNode;
			entryNode = entryNode.next;
		}
	}

	@Override
	public Object clone() {
		XulPriorityPropMap<T> obj = new XulPriorityPropMap<T>();
		obj.copy(this);
		return obj;
	}

	public static final class MapEntry<T extends XulProp> {
		T prop;
		int state;
		MapEntry<T> next;

		static MapEntry _entries = null;

		static private void recycleEntry(MapEntry entry) {
			entry.next = _entries;
			entry.prop = null;
			_entries = entry;
		}

		public MapEntry(T prop, int state) {
			this.state = state;
			this.prop = prop;
		}

		final public void addNext(MapEntry<T> obj) {
			obj.next = next;
			next = obj;
		}

		final public void removeNext() {
			if (next != null) {
				MapEntry entry = next;
				next = next.next;
				recycleEntry(entry);
			}
		}

		public static <T extends XulProp> MapEntry<T> create(T prop, int state) {
			if (_entries != null) {
				MapEntry<T> entry = _entries;
				_entries = entry.next;
				entry.next = null;
				entry.prop = prop;
				entry.state = state;
				return entry;
			}
			return new MapEntry<T>(prop, state);
		}

		final public MapEntry<T> cloneEntry(boolean deep) {
			final MapEntry<T> newEntry = create(prop, state);
			if (deep && next != null) {
				newEntry.next = next.cloneEntry(deep);
			}
			return newEntry;
		}
	}
}
