package ch.ggf.cif.pdf;

import java.util.List;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.engine.api.script.IUpdatableDataSetRow;
import org.eclipse.birt.report.engine.api.script.ScriptException;
import org.eclipse.birt.report.engine.api.script.eventadapter.ScriptedDataSetEventAdapter;
import org.eclipse.birt.report.engine.api.script.instance.IDataSetInstance;

import com.google.common.collect.Lists;

import ch.ggf.common.domain.Container;
import ch.ggf.common.domain.Holder;
import ch.ggf.common.domain.HolderRelation;

public class OwnershipEventAdapter extends ScriptedDataSetEventAdapter {

	protected IReportContext reportContext;
	protected int currentIndex;
	protected List<Object> items;
	protected int numItems = 0;

	private Container container;
	private boolean holderBetweenOthers = false;
	private boolean beneficiaryBetweenOthers = false;
	private Holder holder;

	@Override
	public void beforeOpen(IDataSetInstance dataSet, IReportContext reportContext) throws ScriptException {
		super.beforeOpen(dataSet, reportContext);
		this.reportContext = reportContext;
		currentIndex = 0;
	}

	@Override
	public void open(IDataSetInstance dataSet) throws ScriptException {
		super.open(dataSet);

		container = (Container)reportContext.getParameterValue("container");
		holder = (Holder)reportContext.getParameterValue("holder");

		holderBetweenOthers = holder.hasRelation(HolderRelation.HOLDER) && container.hasMoreThanOneHolderWithRelation(HolderRelation.HOLDER);
		beneficiaryBetweenOthers = holder.hasRelation(HolderRelation.BENEFICIARY) && container.hasMoreThanOneHolderWithRelation(HolderRelation.BENEFICIARY);

		items = Lists.newArrayList();
		items.add(HolderRelation.HOLDER);
		items.add(Container.HOLDER_BETWEEN_OTHERS);
		items.add(HolderRelation.BENEFICIARY);
		items.add(Container.BENEFICIARY_BETWEEN_OTHERS);
		numItems = items.size();
	}

	@Override
	public boolean fetch(IDataSetInstance dataSet, IUpdatableDataSetRow row) throws ScriptException {
		if (numItems == currentIndex) {
			return false;
		}

		int role = (Integer) items.get(currentIndex);
		switch (role) {
			case HolderRelation.HOLDER:
				row.setColumnValue("checked", holderBetweenOthers ? false : holder.hasRelation(role));
				break;
			case Container.HOLDER_BETWEEN_OTHERS:
				row.setColumnValue("checked", holderBetweenOthers);
				break;
			case HolderRelation.BENEFICIARY:
				row.setColumnValue("checked", beneficiaryBetweenOthers ? false : holder.hasRelation(role));
				break;
			case Container.BENEFICIARY_BETWEEN_OTHERS:
				row.setColumnValue("checked", beneficiaryBetweenOthers);
				break;
			default:
				row.setColumnValue("checked", holder.hasRelation(role));
		}
		row.setColumnValue("role", reportContext.getMessage("relation.role." + role, reportContext.getLocale()));
		currentIndex++;
		return true;
	}

}

