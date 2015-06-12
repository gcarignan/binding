package com.netappsid.undoredo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.google.common.collect.ImmutableList;
import com.jgoodies.binding.beans.Observable;
import com.netappsid.binding.beans.CollectionValueModel;
import com.netappsid.binding.beans.support.ChangeSupportFactory;
import com.netappsid.observable.BatchAction;
import com.netappsid.observable.CollectionChangeEvent;
import com.netappsid.observable.CollectionChangeListener;
import com.netappsid.observable.CollectionDifference;
import com.netappsid.observable.ObservableCollection;
import com.netappsid.observable.ObservableCollectionSupport;
import com.netappsid.observable.ObservableCollectionSupportFactory;

public class UndoRedoCollectionValueModel<E, T extends CollectionValueModel<E> & Observable> extends UndoRedoValueModel<T> implements CollectionValueModel<E>
{
	private final ObservableCollectionSupport observableCollectionSupport;
	private final CollectionChangeListener collectionChangeHandler;
	private boolean trackingEnabled = true;

	public UndoRedoCollectionValueModel(UndoRedoManager manager, T valueModel, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			ChangeSupportFactory changeSupportFactory)
	{
		super(manager, valueModel, changeSupportFactory);

		collectionChangeHandler = new DelegateCollectionValueModelCollectionChangeListener();

		observableCollectionSupport = observableCollectionSupportFactory.newObservableCollectionSupport(this);
		getValueModel().addCollectionChangeListener(collectionChangeHandler);
		trackingEnabled = true;
	}

	public void enableCollectionTracking()
	{
		trackingEnabled = true;
	}

	public void disableCollectionTracking()
	{
		trackingEnabled = false;
	}

	protected CollectionChangeListener getUndoRedoManagerPushHandler()
	{
		return collectionChangeHandler;
	}

	@Override
	public void addCollectionChangeListener(CollectionChangeListener listener)
	{
		observableCollectionSupport.addCollectionChangeListener(listener);
	}

	@Override
	public void removeCollectionChangeListener(CollectionChangeListener listener)
	{
		observableCollectionSupport.removeCollectionChangeListener(listener);
	}

	public void undo(CollectionChangeEvent event)
	{
		try
		{
			getValueModel().removeCollectionChangeListener(this.collectionChangeHandler);
			getUndoRedoManager().beginTransaction();
			CollectionDifference difference = event.getDifference();

			ObservableCollection source = getValueModel();
			source.unapply(difference);

		}
		finally
		{
			getValueModel().addCollectionChangeListener(this.collectionChangeHandler);
			getUndoRedoManager().endTransaction();
		}
	}

	public void redo(CollectionChangeEvent event)
	{
		try
		{
			getValueModel().removeCollectionChangeListener(this.collectionChangeHandler);
			getUndoRedoManager().beginTransaction();

			CollectionDifference difference = event.getDifference();

			// Always use the ValueModel's value since when an entity is reloaded, a new collection
			// containing the same objects is recreated
			ObservableCollection source = getValueModel();

			source.apply(difference);

		}
		finally
		{
			getValueModel().addCollectionChangeListener(this.collectionChangeHandler);
			getUndoRedoManager().endTransaction();
		}
	}

	@Override
	public E get(int index)
	{
		return getValueModel().get(index);
	}

	@Override
	public E set(int index, E newValue)
	{
		return getValueModel().set(index, newValue);
	}

	@Override
	public boolean add(E e)
	{
		return getValueModel().add(e);
	}

	@Override
	public boolean addAll(Collection c)
	{
		return getValueModel().addAll(c);
	}

	@Override
	public void clear()
	{
		getValueModel().clear();
	}

	@Override
	public boolean contains(Object e)
	{
		return getValueModel().contains(e);
	}

	@Override
	public boolean containsAll(Collection c)
	{
		return getValueModel().containsAll(c);
	}

	@Override
	public boolean isEmpty()
	{
		return getValueModel().isEmpty();
	}

	@Override
	public Iterator iterator()
	{
		return getValueModel().iterator();
	}

	@Override
	public boolean remove(Object e)
	{
		return getValueModel().remove(e);
	}

	@Override
	public boolean removeAll(Collection c)
	{
		return getValueModel().removeAll(c);
	}

	@Override
	public boolean retainAll(Collection c)
	{
		return getValueModel().retainAll(c);
	}

	@Override
	public int size()
	{
		return getValueModel().size();
	}

	@Override
	public void executeBatchAction(BatchAction action)
	{
		getValueModel().executeBatchAction(action);
	}

	@Override
	public Object[] toArray()
	{
		return getValueModel().toArray();
	}

	@Override
	public Object[] toArray(Object[] a)
	{
		return getValueModel().toArray(a);
	}

	@Override
	public void add(int index, E element)
	{
		getValueModel().add(index, element);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c)
	{
		return getValueModel().addAll(index, c);
	}

	@Override
	public int indexOf(Object o)
	{
		return getValueModel().indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o)
	{
		return getValueModel().lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator()
	{
		return getValueModel().listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index)
	{
		return getValueModel().listIterator(index);
	}

	@Override
	public E remove(int index)
	{
		return getValueModel().remove(index);
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex)
	{
		return getValueModel().subList(fromIndex, toIndex);
	}

	@Override
	public void apply(CollectionDifference<E> difference)
	{
		getValueModel().apply(difference);
	}

	@Override
	public void unapply(CollectionDifference<E> difference)
	{
		getValueModel().unapply(difference);
	}

	@Override
	public ImmutableList<CollectionChangeListener<E>> getCollectionChangeListeners()
	{
		return observableCollectionSupport.getCollectionChangeListeners();
	}

	@Override
	public void dispose()
	{
		T valueModel = getValueModel();

		if (valueModel != null)
		{
			valueModel.removeCollectionChangeListener(collectionChangeHandler);
		}
	}

	protected final class DelegateCollectionValueModelCollectionChangeListener implements CollectionChangeListener
	{
		@Override
		public void onCollectionChange(CollectionChangeEvent event)
		{
			if (trackingEnabled)
			{
				getUndoRedoManager().push(new CollectionChangeOperation(UndoRedoCollectionValueModel.this, event));
			}
			CollectionChangeEvent newCollectionChangeEvent = observableCollectionSupport.newCollectionChangeEvent(event.getDifference());
			observableCollectionSupport.fireCollectionChangeEvent(newCollectionChangeEvent);
		}
	}
}
