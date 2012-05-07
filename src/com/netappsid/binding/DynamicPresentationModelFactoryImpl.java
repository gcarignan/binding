package com.netappsid.binding;

import java.util.Map;

public class DynamicPresentationModelFactoryImpl implements DynamicPresentationModelFactory
{
	@Override
	public DynamicPresentationModel newDynamicPresentationModel(PresentationModel parentModel, String propertyName)
	{
		DynamicPresentationModel dynamicPresentationModel = new DynamicPresentationModel(parentModel.getChangeSupportFactory(),
				parentModel.getObservableCollectionSupportFactory(), parentModel.getValueModel(propertyName), Map.class);

		dynamicPresentationModel.setValueModelFactory(new DynamicPresentationModelValueModelFactoryImpl());
		dynamicPresentationModel.setMapBeanChangeHandler(new DefaultMapBeanChangeHandler(dynamicPresentationModel));
		dynamicPresentationModel.setMappedValueChangeHandler(new DefaultMappedValueChangeHandler(dynamicPresentationModel));

		return dynamicPresentationModel;
	}
}
