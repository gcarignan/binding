package com.netappsid.binding.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Lists;
import com.jgoodies.binding.beans.PropertyUnboundException;
import com.jgoodies.binding.value.ValueModel;
import com.netappsid.binding.beans.support.ChangeSupportFactory;
import com.netappsid.binding.value.AbstractValueModel;
import com.netappsid.binding.value.IndexedCollectionValueModel;
import com.netappsid.binding.value.ValueHolder;
import com.netappsid.observable.ObservableCollectionSupportFactory;
import com.netappsid.validate.Validate;

public class BeanAdapter extends Bean
{
	private final ChangeSupportFactory changeSupportFactory;
	private final ObservableCollectionSupportFactory observableCollectionSupportFactory;
	private final ValueModel beanChannel;
	private final Class<?> beanClass;
	private final Map<String, SimplePropertyAdapter> propertyAdapters;
	private final Map<String, CollectionValueModel> collectionValueModels;
	private final IndirectPropertyChangeSupport indirectChangeSupport;
	private PropertyChangeListener propertyChangeHandler;
	private BeanChangeHandler beanChangeHandler;
	private Object storedOldBean;
	private boolean disposed = false;

	public BeanAdapter(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory, Class<?> beanClass)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, (ValueModel) null, beanClass);
	}

	public BeanAdapter(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory, Object bean,
			Class<?> beanClass)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, new ValueHolder(changeSupportFactory, bean, true), beanClass);
	}

	public BeanAdapter(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			ValueModel beanChannel, Class<?> beanClass)
	{
		super(changeSupportFactory);
		this.changeSupportFactory = changeSupportFactory;
		this.observableCollectionSupportFactory = observableCollectionSupportFactory;
		this.beanChannel = beanChannel != null ? beanChannel : new ValueHolder(changeSupportFactory, null, true);
		this.beanClass = Validate.notNull(beanClass);
		this.propertyAdapters = new HashMap<String, SimplePropertyAdapter>();
		this.collectionValueModels = new HashMap<String, CollectionValueModel>();
		this.indirectChangeSupport = new IndirectPropertyChangeSupport(this.beanChannel);
		this.propertyChangeHandler = new PropertyChangeHandler(this);
		this.beanChangeHandler = new BeanChangeHandler(this);
		this.beanChannel.addValueChangeListener(beanChangeHandler);
		this.storedOldBean = getBean();

		checkBeanChannelIdentityCheck(this.beanChannel);
		addChangeHandlerTo(getBean());
	}

	public Class<?> getBeanClass()
	{
		return beanClass;
	}

	public ValueModel getBeanChannel()
	{
		return beanChannel;
	}

	public Object getBean()
	{
		return beanChannel.getValue();
	}

	public void setBean(Object newBean)
	{
		beanChannel.setValue(newBean);
	}

	public Object getValue(String propertyName)
	{
		return getValueModel(propertyName).getValue();
	}

	public void setValue(String propertyName, Object newValue)
	{
		getValueModel(propertyName).setValue(newValue);
	}

	public SimplePropertyAdapter getValueModel(String propertyName)
	{
		Validate.notNull(propertyName, "The property name must not be null.");

		SimplePropertyAdapter registeredPropertyAdapter = propertyAdapters.get(propertyName);

		if (registeredPropertyAdapter == null)
		{
			registeredPropertyAdapter = new SimplePropertyAdapter(this, propertyName);
			propertyAdapters.put(propertyName, registeredPropertyAdapter);
		}

		return registeredPropertyAdapter;
	}

	public CollectionValueModel getCollectionValueModel(String propertyName)
	{
		CollectionValueModel collectionValueModel = getCollectionValueModelsCache().get(propertyName);

		if (collectionValueModel == null)
		{
			SimplePropertyAdapter propertyAdapter = getValueModel(propertyName);
			collectionValueModel = new IndexedCollectionValueModel(propertyAdapter, getChangeSupportFactory(), observableCollectionSupportFactory);
			getCollectionValueModelsCache().put(propertyName, collectionValueModel);
		}

		return collectionValueModel;
	}

	public synchronized void addBeanPropertyChangeListener(PropertyChangeListener listener)
	{
		if (listener != null)
		{
			indirectChangeSupport.addPropertyChangeListener(listener);
		}
	}

	public synchronized void removeBeanPropertyChangeListener(PropertyChangeListener listener)
	{
		if (listener != null)
		{
			indirectChangeSupport.removePropertyChangeListener(listener);
		}
	}

	public synchronized void addBeanPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		if (listener != null)
		{
			indirectChangeSupport.addPropertyChangeListener(propertyName, listener);
		}
	}

	public synchronized void removeBeanPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		if (listener != null)
		{
			indirectChangeSupport.removePropertyChangeListener(propertyName, listener);
		}
	}

	public synchronized PropertyChangeListener[] getBeanPropertyChangeListeners()
	{
		return indirectChangeSupport.getPropertyChangeListeners();
	}

	public synchronized PropertyChangeListener[] getBeanPropertyChangeListeners(String propertyName)
	{
		return indirectChangeSupport.getPropertyChangeListeners(propertyName);
	}

	public synchronized void release()
	{
		removeChangeHandlerFrom(getBean());

		// Ensure to dispose every CollectionValueModel in order
		// to release listeners on Collections on the bean itself
		for (CollectionValueModel collectionValueModel : getCollectionValueModelsCache().values())
		{
			collectionValueModel.dispose();
		}

		indirectChangeSupport.removeAll();
		removeChangeHandlerFrom(storedOldBean);
	}

	private void addChangeHandlerTo(Object bean)
	{
		if (bean != null)
		{
			if (BeanUtils.supportsBoundProperties(bean.getClass()))
			{
				BeanUtils.addPropertyChangeListener(bean, bean.getClass(), propertyChangeHandler);
			}
			else
			{
				throw new PropertyUnboundException(
						"The bean must provide support for listening on property changes as described in section 7.4.5 of the Java Bean Specification.");
			}
		}
	}

	protected ChangeSupportFactory getChangeSupportFactory()
	{
		return changeSupportFactory;
	}

	private void removeChangeHandlerFrom(Object bean)
	{
		if (bean != null && propertyChangeHandler != null)
		{
			BeanUtils.removePropertyChangeListener(bean, bean.getClass(), propertyChangeHandler);
		}
	}

	private void checkBeanChannelIdentityCheck(ValueModel valueModel)
	{
		if (valueModel instanceof ValueHolder && !((ValueHolder) valueModel).isIdentityCheckEnabled())
		{
			throw new IllegalArgumentException("The bean channel must have the identity check enabled.");
		}
	}

	protected Map<String, CollectionValueModel> getCollectionValueModelsCache()
	{
		return collectionValueModels;
	}

	private static final class BeanChangeHandler implements PropertyChangeListener
	{
		private BeanAdapter beanAdapter;

		public BeanChangeHandler(BeanAdapter beanAdapter)
		{
			this.beanAdapter = beanAdapter;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			final Object newBean = evt.getNewValue() != null ? evt.getNewValue() : beanAdapter.getBean();

			setBean(beanAdapter.storedOldBean, newBean);
			beanAdapter.storedOldBean = newBean;
		}

		private void setBean(Object oldBean, Object newBean)
		{
			beanAdapter.fireIdentityPropertyChange(PROPERTYNAME_BEFORE_BEAN, oldBean, newBean);
			beanAdapter.removeChangeHandlerFrom(oldBean);
			forwardAllAdaptedValuesChanged(oldBean, newBean);
			beanAdapter.addChangeHandlerTo(newBean);
			beanAdapter.fireIdentityPropertyChange(PROPERTYNAME_BEAN, oldBean, newBean);
			beanAdapter.fireIdentityPropertyChange(PROPERTYNAME_AFTER_BEAN, oldBean, newBean);
		}

		private void forwardAllAdaptedValuesChanged(Object oldBean, Object newBean)
		{
			for (SimplePropertyAdapter adapter : Lists.newArrayList(beanAdapter.propertyAdapters.values()))
			{
				adapter.setBean(oldBean, newBean);
			}
		}

		public void dispose()
		{
			beanAdapter = null;
		}
	}

	private static final class PropertyChangeHandler implements PropertyChangeListener
	{
		private BeanAdapter beanAdapter;

		public PropertyChangeHandler(BeanAdapter beanAdapter)
		{
			this.beanAdapter = beanAdapter;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt)
		{
			if (evt.getPropertyName() == null)
			{
				forwardAllAdaptedValuesChanged();
			}
			else
			{
				final AbstractValueModel adapter = beanAdapter.propertyAdapters.get(evt.getPropertyName());

				if (adapter != null)
				{
					adapter.fireValueChange(evt.getOldValue(), evt.getNewValue(), true);
				}
			}
		}

		private void forwardAllAdaptedValuesChanged()
		{
			final Object currentBean = beanAdapter.getBean();

			for (SimplePropertyAdapter adapter : Lists.newArrayList(beanAdapter.propertyAdapters.values()))
			{
				adapter.fireChange(currentBean);
			}
		}

		public void dispose()
		{
			beanAdapter = null;
		}
	}

	public void dispose()
	{
		if (!disposed)
		{
			propertyAdapters.clear();
			collectionValueModels.clear();

			if (storedOldBean instanceof BeanAdapter)
			{
				((BeanAdapter) storedOldBean).dispose();
			}

			if (propertyChangeHandler != null)
			{
				((PropertyChangeHandler) propertyChangeHandler).dispose();
				propertyChangeHandler = null;
			}

			if (beanChangeHandler != null)
			{
				beanChangeHandler.dispose();
				beanChangeHandler = null;
			}

			if (storedOldBean != null && storedOldBean instanceof BeanAdapter)
			{
				((BeanAdapter) storedOldBean).dispose();
			}
			storedOldBean = null;
			disposed = true;
		}
	}

	public static final String PROPERTYNAME_BEFORE_BEAN = "beforeBean";
	public static final String PROPERTYNAME_BEAN = "bean";
	public static final String PROPERTYNAME_AFTER_BEAN = "afterBean";
}
