package com.netappsid.binding;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import com.jgoodies.binding.value.ValueModel;
import com.netappsid.binding.beans.BeanAdapter;
import com.netappsid.binding.beans.support.ChangeSupportFactory;
import com.netappsid.binding.state.State;
import com.netappsid.binding.state.StateModel;
import com.netappsid.binding.state.StatePropertyChangeEvent;
import com.netappsid.binding.value.ValueHolder;
import com.netappsid.observable.ObservableCollectionSupportFactory;
import com.netappsid.undoredo.UndoRedoManager;

/**
 * 
 * 
 * @author Eric Belanger
 * @author NetAppsID Inc.
 */
@SuppressWarnings("serial")
public class DefaultPresentationModel extends PresentationModel
{
	public static final String PROPERTYNAME_BEAN = "bean";

	private boolean disposed = false;
	private BeanAdapter beanAdapter;
	private final StateModel stateModel;
	private UndoRedoManager undoRedoManager;

	private BeanChangeHandler beanChangeHandler;
	private UpdateStateOnBeanPropertyChangeHandler stateUpdaterOnBeanPropertyChange;

	public DefaultPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			Class<?> beanClass)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, beanClass, new ValueHolder(changeSupportFactory, null, true));
	}

	public DefaultPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			Class<?> beanClass, Object bean)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, beanClass, bean, null);
	}

	public DefaultPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			Class<?> beanClass, Object bean, UndoRedoManager undoRedoManager)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, beanClass, new ValueHolder(changeSupportFactory, bean, true), undoRedoManager);
	}

	public DefaultPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			Class<?> beanClass, ValueModel beanChannel)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, beanClass, beanChannel, null);
	}

	public DefaultPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			Class<?> beanClass, ValueModel beanChannel, UndoRedoManager undoRedoManager)
	{
		super(changeSupportFactory, observableCollectionSupportFactory);
		this.undoRedoManager = undoRedoManager;
		this.beanAdapter = new BeanAdapter(changeSupportFactory, observableCollectionSupportFactory, beanChannel, beanClass);
		this.stateModel = new StateModel(changeSupportFactory);

		setBeanClass(beanClass);
		beanChangeHandler = new BeanChangeHandler(this);
		beanAdapter.addPropertyChangeListener(BeanAdapter.PROPERTYNAME_BEAN, beanChangeHandler);
		stateUpdaterOnBeanPropertyChange = new UpdateStateOnBeanPropertyChangeHandler(this.stateModel);
		beanAdapter.addBeanPropertyChangeListener(stateUpdaterOnBeanPropertyChange);
	}

	@Override
	public Object getBean()
	{
		return beanAdapter.getBean();
	}

	@Override
	public ValueModel getBeanChannel()
	{
		return beanAdapter.getBeanChannel();
	}

	@Override
	public Object getValue(String propertyName)
	{
		return getValueModel(propertyName).getValue();
	}

	@Override
	public ValueModel getValueModel(String propertyName)
	{
		ValueModel valueModel = null;
		int index = propertyName.lastIndexOf('.');

		if (index == -1)
		{
			valueModel = beanAdapter.getValueModel(propertyName);
		}
		else
		{
			valueModel = getSubModel(propertyName.substring(0, index)).getValueModel(propertyName.substring(index + 1, propertyName.length()));
		}

		return valueModel;
	}

	/**
	 * Replaces the current {@link UpdateStateOnBeanPropertyChangeHandler} on the inner beanAdapter with the provided one.
	 * 
	 * @param stateUpdaterOnBeanPropertyChange
	 */
	public void setStateUpdaterOnBeanPropertyChange(UpdateStateOnBeanPropertyChangeHandler stateUpdaterOnBeanPropertyChange)
	{
		beanAdapter.removeBeanPropertyChangeListener(this.stateUpdaterOnBeanPropertyChange);
		this.stateUpdaterOnBeanPropertyChange = stateUpdaterOnBeanPropertyChange;
		beanAdapter.addBeanPropertyChangeListener(this.stateUpdaterOnBeanPropertyChange);

	}

	@Override
	public void releaseBeanListeners()
	{
		beanAdapter.release();
	}

	@Override
	public void removeBeanPropertyChangeListener(PropertyChangeListener listener)
	{
		beanAdapter.removeBeanPropertyChangeListener(listener);
	}

	@Override
	public void removeBeanPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		beanAdapter.removeBeanPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void setBean(Object newBean)
	{
		beanAdapter.setBean(newBean);
	}

	@Override
	public void setValue(String propertyName, Object newValue)
	{
		getValueModel(propertyName).setValue(newValue);
	}

	@Override
	public StateModel getStateModel()
	{
		return stateModel;
	}

	public UndoRedoManager getUndoRedoManager()
	{
		return undoRedoManager;
	}

	@Override
	public void dispose()
	{
		if (!disposed)
		{
			disposed = true;

			for (PresentationModel subModel : getSubModels().values())
			{
				subModel.dispose();
			}

			getSubModels().clear();

			undoRedoManager = null;

			beanAdapter.removeBeanPropertyChangeListener(stateUpdaterOnBeanPropertyChange);
			beanAdapter.removePropertyChangeListener(BeanAdapter.PROPERTYNAME_BEAN, beanChangeHandler);
			beanAdapter.release();
			beanAdapter.dispose();
			beanAdapter = null;

			if (stateUpdaterOnBeanPropertyChange != null)
			{
				stateUpdaterOnBeanPropertyChange.dispose();
				stateUpdaterOnBeanPropertyChange = null;
			}
			if (beanChangeHandler != null)
			{
				beanChangeHandler.dispose();
				beanChangeHandler = null;
			}
		}
	}

	/**
	 * A listener whose responsibility is to update a {@link StateModel}'s state when it receives a {@link StatePropertyChangeEvent} which affects state.<br/>
	 * It was made static so it can be extended to customize its behavior, but this only works if the {@link DefaultPresentationModel}'s stateModel can never
	 * change instance (which is the case at the time of writing this).
	 * 
	 */
	public static class UpdateStateOnBeanPropertyChangeHandler implements PropertyChangeListener
	{
		private StateModel stateModel;

		public UpdateStateOnBeanPropertyChangeHandler(StateModel stateModel)
		{
			this.stateModel = stateModel;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			// If event should impact state, change the state to dirty
			if (evt instanceof StatePropertyChangeEvent && ((StatePropertyChangeEvent) evt).isAffectingState())
			{
				stateModel.setState(State.DIRTY);
			}
		}

		public void dispose()
		{
			stateModel = null;
		}
	}

	/**
	 * Responsible for bubbling bean change events to listeners on the PresentationModel.
	 * 
	 * @author Eric Belanger
	 * @author NetAppsID Inc.
	 * @version $Revision: 1.16 $
	 */
	private static final class BeanChangeHandler implements PropertyChangeListener
	{
		private DefaultPresentationModel defaultPresentationModel;

		public BeanChangeHandler(DefaultPresentationModel defaultPresentationModel)
		{
			this.defaultPresentationModel = defaultPresentationModel;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			defaultPresentationModel.fireIdentityPropertyChange(PROPERTYNAME_BEAN, evt.getOldValue(), evt.getNewValue());
		}

		public void dispose()
		{
			defaultPresentationModel = null;
		}
	}
}
