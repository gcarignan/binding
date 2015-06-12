package com.netappsid.undoredo;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.beans.Observable;
import com.jgoodies.binding.value.ValueModel;
import com.netappsid.binding.beans.support.ChangeSupportFactory;
import com.netappsid.binding.beans.support.IdentityPropertyChangeSupport;
import com.netappsid.binding.value.AbstractValueModel;
import com.netappsid.binding.value.BoundValueModel;
import com.netappsid.observable.ObservableByName;

public class UndoRedoValueModel<T extends ValueModel & Observable> implements BoundValueModel, ObservableByName, ReinitializableValueModel
{
	private final T valueModel;
	private final UndoRedoManager undoRedoManager;
	private final IdentityPropertyChangeSupport propertyChangeSupport;

	public UndoRedoValueModel(UndoRedoManager manager, T valueModel, ChangeSupportFactory changeSupportFactory)
	{
		this.undoRedoManager = manager;
		this.valueModel = valueModel;
		this.propertyChangeSupport = changeSupportFactory.createIdentityPropertyChangeSupport(this);
		valueModel.addValueChangeListener(new DelegateValueModelValueChangeListener());
	}

	@Override
	public void addValueChangeListener(PropertyChangeListener listener)
	{
		addPropertyChangeListener(AbstractValueModel.PROPERTYNAME_VALUE, listener);
	}

	@Override
	public Object getValue()
	{
		return getValueModel().getValue();
	}

	@Override
	public void removeValueChangeListener(PropertyChangeListener listener)
	{
		removePropertyChangeListener(AbstractValueModel.PROPERTYNAME_VALUE, listener);
	}

	@Override
	public void setValue(Object value)
	{
		T valueModel = getValueModel();

		Object valueOld;
		if (valueModel instanceof UndoSupport)
		{
			valueOld = ((UndoSupport) valueModel).getUndoableValue(value);
		}
		else
		{
			valueOld = valueModel.getValue();
		}

		UndoRedoValue undoRedoValue = new UndoRedoValue(valueOld, value);
		UndoRedoValueModelOperation undoRedoValueModelOperation = new UndoRedoValueModelOperation(this, undoRedoValue);
		getUndoRedoManager().push(undoRedoValueModelOperation);
		getUndoRedoManager().beginTransaction();
		getValueModel().setValue(value);
		getUndoRedoManager().endTransaction();
	}

	@Override
	public void reinitialize(Object value)
	{
		if (getValueModel() instanceof ReinitializableValueModel)
		{
			((ReinitializableValueModel) getValueModel()).reinitialize(value);
		}
		else
		{
			getValueModel().setValue(value);
		}
	}

	public void redo(UndoRedoValue undoRedoValue)
	{
		getValueModel().setValue(undoRedoValue.getNewValue());
	}

	public void undo(UndoRedoValue undoRedoValue)
	{
		getValueModel().setValue(undoRedoValue.getOldValue());
	}

	public ValueModel getDelegate()
	{
		return getValueModel();
	}

	protected UndoRedoManager getUndoRedoManager()
	{
		return undoRedoManager;
	}

	protected T getValueModel()
	{
		return valueModel;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener)
	{
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener)
	{
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	@Override
	public String getPropertyName()
	{
		if (getValueModel() instanceof BoundValueModel)
		{
			return ((BoundValueModel) getValueModel()).getPropertyName();
		}

		return BoundValueModel.UNKNOWN_PROPERTY;
	}

	private final class DelegateValueModelValueChangeListener implements PropertyChangeListener
	{
		@Override
		public void propertyChange(PropertyChangeEvent event)
		{
			propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(UndoRedoValueModel.this, event.getPropertyName(), event.getOldValue(), event
					.getNewValue()));
		}
	}
}
