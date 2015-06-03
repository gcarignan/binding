package com.netappsid.binding;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.jgoodies.binding.value.ValueModel;
import com.netappsid.binding.beans.support.ChangeSupportFactory;
import com.netappsid.binding.state.StateModel;
import com.netappsid.binding.value.ValueHolder;
import com.netappsid.observable.ObservableCollectionSupportFactory;
import com.netappsid.validate.Validate;

@SuppressWarnings("serial")
public class DynamicPresentationModel extends PresentationModel
{
	private DynamicPresentationModelValueModelFactory valueModelFactory;

	private DynamicPresentationModelMappedValueChangedHandler mappedValueChangeHandler;
	private DynamicPresentationModelMapBeanChangeHandler mapBeanChangeHandler;

	private final ValueModel mapChannel;
	private final PropertyChangeSupport propertyChangeSupport;
	private final StateModel stateModel;
	private final Map<String, ValueModel> namesToValueModels = new HashMap<String, ValueModel>();
	private final Map<ValueModel, String> valueModelToNames = new HashMap<ValueModel, String>();

	public DynamicPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, new ValueHolder(changeSupportFactory), Map.class);
	}

	public DynamicPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			Map<String, ?> map)
	{
		this(changeSupportFactory, observableCollectionSupportFactory, new ValueHolder(changeSupportFactory, map), Map.class);
	}

	public DynamicPresentationModel(ChangeSupportFactory changeSupportFactory, ObservableCollectionSupportFactory observableCollectionSupportFactory,
			ValueModel mapChannel, Class<?> beanClass)
	{
		super(changeSupportFactory, observableCollectionSupportFactory);

		this.mapChannel = Validate.notNull(mapChannel, "Map Channel cannot be null.");
		this.propertyChangeSupport = new PropertyChangeSupport(mapChannel);
		this.stateModel = new StateModel(changeSupportFactory);

		setBeanClass(beanClass);
	}

	@Override
	public Object getBean()
	{
		return getBeanChannel().getValue();
	}

	@Override
	public ValueModel getBeanChannel()
	{
		return mapChannel;
	}

	@Override
	public Object getValue(String propertyName)
	{
		return getValueModel(propertyName).getValue();
	}

	@Override
	public ValueModel getValueModel(String propertyName)
	{
		if (getBean() == null)
		{
			setBean(new HashMap<String, Object>());
		}

		ValueModel valueModel = namesToValueModels.get(propertyName);

		if (valueModel == null)
		{
			valueModel = registerValueModel(propertyName);
		}

		return valueModel;
	}

	@Override
	public void releaseBeanListeners()
	{
		PropertyChangeListener[] propertyChangeListeners = getPropertyChangeListeners();

		if (propertyChangeListeners != null)
		{
			for (PropertyChangeListener listener : propertyChangeListeners)
			{
				if (listener instanceof PropertyChangeListenerProxy)
				{
					removePropertyChangeListener(((PropertyChangeListenerProxy) listener).getPropertyName(), listener);
				}
				else
				{
					removePropertyChangeListener(listener);
				}
			}
		}
	}

	@Override
	public void removeBeanPropertyChangeListener(PropertyChangeListener listener)
	{
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removeBeanPropertyChangeListener(String propertyName, PropertyChangeListener listener)
	{
		propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
	}

	@Override
	public void setBean(Object newBean)
	{
		if (newBean != null && !(newBean instanceof Map<?, ?>))
		{
			throw new IllegalArgumentException("Can only set a Map or a null value for the new bean.");
		}

		getBeanChannel().setValue(newBean);
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

	protected Map<ValueModel, String> getValueModelToNames()
	{
		return ImmutableMap.copyOf(valueModelToNames);
	}

	public Map<String, ValueModel> getNamesToValueModels()
	{
		return ImmutableMap.copyOf(namesToValueModels);
	}

	@SuppressWarnings("unchecked")
	private ValueModel registerValueModel(String propertyName)
	{
		Map map = (Map) getBean();

		if (!map.containsKey(propertyName))
		{
			map.put(propertyName, (Object) null);
		}

		Object mapValue = map.get(propertyName);

		ValueModel valueModel = getValueModelFactory().createValueModelForMapValue(propertyName, mapValue, getChangeSupportFactory());
		valueModel.addValueChangeListener(getMappedValueChangeHandler());
		namesToValueModels.put(propertyName, valueModel);
		valueModelToNames.put(valueModel, propertyName);

		return valueModel;
	}

	/**
	 * Handler to detect the change of one of the Map Entry's ValueModel mapped to the value e.g.: The CustomAttribute changed
	 * 
	 * @return
	 */
	protected DynamicPresentationModelMappedValueChangedHandler getMappedValueChangeHandler()
	{
		return mappedValueChangeHandler;
	}

	public void setMappedValueChangeHandler(DynamicPresentationModelMappedValueChangedHandler mappedValueChangeHandler)
	{
		this.mappedValueChangeHandler = mappedValueChangeHandler;
	}

	/**
	 * Handler to detect the change of the Map object itself
	 * 
	 * @return
	 */
	protected DynamicPresentationModelMapBeanChangeHandler getMapBeanChangeHandler()
	{
		return mapBeanChangeHandler;
	}

	public void setMapBeanChangeHandler(DynamicPresentationModelMapBeanChangeHandler mapBeanChangeHandler)
	{
		mapChannel.removeValueChangeListener(this.mapBeanChangeHandler);
		this.mapBeanChangeHandler = mapBeanChangeHandler;
		mapChannel.addValueChangeListener(this.mapBeanChangeHandler);
	}

	protected DynamicPresentationModelValueModelFactory getValueModelFactory()
	{
		return valueModelFactory;
	}

	public void setValueModelFactory(DynamicPresentationModelValueModelFactory valueModelFactory)
	{
		this.valueModelFactory = valueModelFactory;
	}

	@Override
	public void dispose()
	{
		// TODO Auto-generated method stub

	}
}
