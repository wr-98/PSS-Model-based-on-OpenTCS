/**
 * Copyright (c) The openTCS Authors.
 *
 * This program is free software and subject to the MIT license. (For details,
 * see the licensing information (LICENSE.txt) you should have received with
 * this copy of the software.)
 */
package org.opentcs.kernel.services;

import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Set;
import javax.inject.Inject;
import org.opentcs.access.KernelRuntimeException;
import org.opentcs.components.kernel.services.InternalVehicleService;
import org.opentcs.components.kernel.services.TCSObjectService;
import org.opentcs.components.kernel.services.VehicleService;
import org.opentcs.customizations.kernel.GlobalSyncObject;
import org.opentcs.data.ObjectUnknownException;
import org.opentcs.data.TCSObjectEvent;
import org.opentcs.data.TCSObjectReference;
import org.opentcs.data.model.Point;
import org.opentcs.data.model.Triple;
import org.opentcs.data.model.Vehicle;
import org.opentcs.data.order.OrderSequence;
import org.opentcs.data.order.TransportOrder;
import org.opentcs.drivers.vehicle.AdapterCommand;
import org.opentcs.drivers.vehicle.LoadHandlingDevice;
import org.opentcs.drivers.vehicle.VehicleCommAdapterDescription;
import org.opentcs.drivers.vehicle.management.AttachmentInformation;
import org.opentcs.drivers.vehicle.management.VehicleProcessModelTO;
import org.opentcs.kernel.extensions.controlcenter.vehicles.AttachmentManager;
import org.opentcs.kernel.extensions.controlcenter.vehicles.VehicleEntry;
import org.opentcs.kernel.extensions.controlcenter.vehicles.VehicleEntryPool;
import org.opentcs.kernel.vehicles.LocalVehicleControllerPool;
import org.opentcs.kernel.vehicles.VehicleCommAdapterRegistry;
import org.opentcs.kernel.workingset.Model;
import org.opentcs.kernel.workingset.TCSObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//modified by Henry
import org.opentcs.data.model.Location;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import org.opentcs.components.kernel.services.ChangeTrackService;
import org.opentcs.components.kernel.services.DataBaseService;
import org.opentcs.data.TCSObject;
import org.opentcs.data.model.Bin;
import org.opentcs.data.order.TransportOrderBin;

/**
 * This class is the standard implementation of the {@link VehicleService} interface.
 *
 * @author Martin Grzenia (Fraunhofer IML)
 */
public class StandardVehicleService
    extends AbstractTCSObjectService
    implements InternalVehicleService {

  /**
   * This class' logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(StandardVehicleService.class);
  /**
   * A global object to be used for synchronization within the kernel.
   */
  private final Object globalSyncObject;
  /**
   * The container of all course model and transport order objects.
   */
  private final TCSObjectPool globalObjectPool;
  /**
   * A pool of vehicle controllers.
   */
  private final LocalVehicleControllerPool vehicleControllerPool;
  /**
   * A pool of vehicle entries.
   */
  private final VehicleEntryPool vehicleEntryPool;
  /**
   * The attachment manager.
   */
  private final AttachmentManager attachmentManager;
  /**
   * The registry for all communication adapters.
   */
  private final VehicleCommAdapterRegistry commAdapterRegistry;
  /**
   * The model facade to the object pool.
   */
  private final Model model;
  /**
   * The data base service.
   * modified by Henry
   */
  private final DataBaseService dataBaseService;
  /**
   * The change track service.
   * modified by Henry
   */
  private final ChangeTrackService changeTrackService;
  /**
   * Creates a new instance.
   *
   * @param objectService The tcs object service.
   * @param globalSyncObject The kernel threads' global synchronization object.
   * @param globalObjectPool The object pool to be used.
   * @param vehicleControllerPool The controller pool to be used.
   * @param vehicleEntryPool The pool of vehicle entries to be used.
   * @param attachmentManager The attachment manager.
   * @param commAdapterRegistry The registry for all communication adapters.
   * @param model The model to be used.
   * @param dataBaseService The data base service to be used.
   * @param changeTrackService The change track service.
   */
  @Inject
  public StandardVehicleService(TCSObjectService objectService,
                                @GlobalSyncObject Object globalSyncObject,
                                TCSObjectPool globalObjectPool,
                                LocalVehicleControllerPool vehicleControllerPool,
                                VehicleEntryPool vehicleEntryPool,
                                AttachmentManager attachmentManager,
                                VehicleCommAdapterRegistry commAdapterRegistry,
                                Model model,
                                DataBaseService dataBaseService,
                                ChangeTrackService changeTrackService) {
    super(objectService);
    this.globalSyncObject = requireNonNull(globalSyncObject, "globalSyncObject");
    this.globalObjectPool = requireNonNull(globalObjectPool, "globalObjectPool");
    this.vehicleControllerPool = requireNonNull(vehicleControllerPool, "vehicleControllerPool");
    this.vehicleEntryPool = requireNonNull(vehicleEntryPool, "vehicleEntryPool");
    this.attachmentManager = requireNonNull(attachmentManager, "attachmentManager");
    this.commAdapterRegistry = requireNonNull(commAdapterRegistry, "commAdapterRegistry");
    this.model = requireNonNull(model, "model");
    this.dataBaseService = requireNonNull(dataBaseService,"dataBaseService");
    this.changeTrackService = requireNonNull(changeTrackService,"changeTrackService");
  }

  @Override
  public void updateVehicleEnergyLevel(TCSObjectReference<Vehicle> ref, int energyLevel)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleEnergyLevel(ref, energyLevel);
    }
  }

  @Override
  public void updateVehicleLoadHandlingDevices(TCSObjectReference<Vehicle> ref,
                                               List<LoadHandlingDevice> devices)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleLoadHandlingDevices(ref, devices);
    }
  }

  @Override
  public void updateVehicleNextPosition(TCSObjectReference<Vehicle> vehicleRef,
                                        TCSObjectReference<Point> pointRef)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleNextPosition(vehicleRef, pointRef);
    }
  }

  @Override
  public void updateVehicleOrderSequence(TCSObjectReference<Vehicle> vehicleRef,
                                         TCSObjectReference<OrderSequence> sequenceRef)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleOrderSequence(vehicleRef, sequenceRef);
    }
  }

  @Override
  public void updateVehicleOrientationAngle(TCSObjectReference<Vehicle> ref, double angle)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleOrientationAngle(ref, angle);
    }
  }

  @Override
  public void updateVehiclePosition(TCSObjectReference<Vehicle> vehicleRef,
                                    TCSObjectReference<Point> pointRef)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.debug("Vehicle {} has reached point {}.", vehicleRef, pointRef);
      model.setVehiclePosition(vehicleRef, pointRef);
    }
  }

  @Override
  public void updateVehiclePrecisePosition(TCSObjectReference<Vehicle> ref, Triple position)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehiclePrecisePosition(ref, position);
    }
  }

  @Override
  public void updateVehicleProcState(TCSObjectReference<Vehicle> ref, Vehicle.ProcState state)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      LOG.debug("Updating procState of vehicle {} to {}...", ref.getName(), state);
      model.setVehicleProcState(ref, state);
    }
  }

  @Override
  public void updateVehicleRechargeOperation(TCSObjectReference<Vehicle> ref,
                                             String rechargeOperation)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleRechargeOperation(ref, rechargeOperation);
    }
  }

  @Override
  public void updateVehicleRouteProgressIndex(TCSObjectReference<Vehicle> ref, int index)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleRouteProgressIndex(ref, index);
    }
  }

  @Override
  public void updateVehicleState(TCSObjectReference<Vehicle> ref, Vehicle.State state)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleState(ref, state);
    }
  }

  @Override
  public void updateVehicleTransportOrder(TCSObjectReference<Vehicle> vehicleRef,
                                          TCSObjectReference<TransportOrder> orderRef)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleTransportOrder(vehicleRef, orderRef);
    }
  }

  @Override
  public void attachCommAdapter(TCSObjectReference<Vehicle> ref,
                                VehicleCommAdapterDescription description)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      attachmentManager.attachAdapterToVehicle(ref.getName(),
                                               commAdapterRegistry.findFactoryFor(description));
    }
  }

  @Override
  public void disableCommAdapter(TCSObjectReference<Vehicle> ref)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      VehicleEntry entry = vehicleEntryPool.getEntryFor(ref.getName());
      if (entry == null) {
        throw new IllegalArgumentException("No vehicle entry found for" + ref.getName());
      }

      entry.getCommAdapter().disable();
    }
  }

  @Override
  public void enableCommAdapter(TCSObjectReference<Vehicle> ref)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      VehicleEntry entry = vehicleEntryPool.getEntryFor(ref.getName());
      if (entry == null) {
        throw new IllegalArgumentException("No vehicle entry found for " + ref.getName());
      }

      entry.getCommAdapter().enable();
    }
  }

  @Override
  public AttachmentInformation fetchAttachmentInformation(TCSObjectReference<Vehicle> ref)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      return attachmentManager.getAttachmentInformation(ref.getName());
    }
  }

  @Override
  public VehicleProcessModelTO fetchProcessModel(TCSObjectReference<Vehicle> ref)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      VehicleEntry entry = vehicleEntryPool.getEntryFor(ref.getName());
      if (entry == null) {
        throw new IllegalArgumentException("No vehicle entry found for " + ref.getName());
      }

      return entry.getCommAdapter().createTransferableProcessModel();
    }
  }

  @Override
  public void sendCommAdapterCommand(TCSObjectReference<Vehicle> ref, AdapterCommand command)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      vehicleControllerPool
          .getVehicleController(ref.getName())
          .sendCommAdapterCommand(command);
    }
  }

  @Override
  public void sendCommAdapterMessage(TCSObjectReference<Vehicle> ref, Object message)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      vehicleControllerPool
          .getVehicleController(ref.getName())
          .sendCommAdapterMessage(message);
    }
  }

  @Override
  public void updateVehicleIntegrationLevel(TCSObjectReference<Vehicle> ref,
                                            Vehicle.IntegrationLevel integrationLevel)
      throws ObjectUnknownException, KernelRuntimeException {
    synchronized (globalSyncObject) {
      Vehicle vehicle = fetchObject(Vehicle.class, ref);

      if (vehicle.isProcessingOrder()
          && (integrationLevel == Vehicle.IntegrationLevel.TO_BE_IGNORED
              || integrationLevel == Vehicle.IntegrationLevel.TO_BE_NOTICED)) {
        throw new IllegalArgumentException(
            String.format("%s: Cannot change integration level to %s while processing orders.",
                          vehicle.getName(),
                          integrationLevel.name())
        );
      }
      
      if(vehicle.getIntegrationLevel() == Vehicle.IntegrationLevel.TO_BE_UTILIZED
          || integrationLevel == Vehicle.IntegrationLevel.TO_BE_UTILIZED)
        changeTrackService.setVehicleStateChanged();

      model.setVehicleIntegrationLevel(ref, integrationLevel);
    }
  }

  @Override
  @Deprecated
  public void updateVehicleProcessableCategories(TCSObjectReference<Vehicle> ref,
                                                 Set<String> processableCategories)
      throws ObjectUnknownException {
    updateVehicleAllowedOrderTypes(ref, processableCategories);
  }

  @Override
  public void updateVehicleAllowedOrderTypes(TCSObjectReference<Vehicle> ref,
                                             Set<String> allowedOrderTypes)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      model.setVehicleAllowedOrderTypes(ref, allowedOrderTypes);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////modified by Henry
  @SuppressWarnings("deprecation")
  @Override
    public void popBinFromLocation(TCSObjectReference<Vehicle> vehicleRef, Location location, boolean afterPick)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      Vehicle vehicle = globalObjectPool.getObject(Vehicle.class, vehicleRef);
      Vehicle previousVehicle = vehicle.clone();
      Location previousLocation = location.clone();
      Bin previousBin = location.pop();
      
      if (previousBin == null){
        LOG.error("{} is catching bin from a empty stack {}",vehicleRef.getName(),location.getName());
        return;
      }
      
      if(afterPick){
        previousBin = updateBinAfterPick(vehicle, previousBin);
      }
      
      Bin currentBin = globalObjectPool.replaceObject(previousBin.withAttachedVehicle(vehicleRef));
      vehicle = globalObjectPool.replaceObject(vehicle.withBin(currentBin));
      location = globalObjectPool.replaceObject(location);
      
      globalObjectPool.emitObjectEvent(location.clone(), previousLocation, TCSObjectEvent.Type.OBJECT_MODIFIED);
      globalObjectPool.emitObjectEvent(vehicle.clone(), previousVehicle, TCSObjectEvent.Type.OBJECT_MODIFIED);
      globalObjectPool.emitObjectEvent(currentBin, previousBin, TCSObjectEvent.Type.OBJECT_MODIFIED);
      
      dataBaseService.updateDataBase();
    }
  }

  @SuppressWarnings("deprecation")
  @Override
    public void pushBinToLocation(TCSObjectReference<Vehicle> vehicleRef, Location location)
      throws ObjectUnknownException {
    synchronized (globalSyncObject) {
      Vehicle vehicle = globalObjectPool.getObject(Vehicle.class, vehicleRef);
      Vehicle previousVehicle = vehicle.clone();
      Location previousLocation = location.clone();
      Bin previousBin = vehicle.getBin();
      Bin currentBin = previousBin
                      .withAttachedLocation(location.getReference())
                      .withPsbTrack(location.getPsbTrack())
                      .withPstTrack(location.getPstTrack())
                      .withBinPosition(location.stackSize());
      
      if(location.push(currentBin)){
        vehicle = globalObjectPool.replaceObject(vehicle.withBin(new Bin("")));
        location = globalObjectPool.replaceObject(location);
        currentBin = globalObjectPool.replaceObject(currentBin);
        
        globalObjectPool.emitObjectEvent(location.clone(), previousLocation, TCSObjectEvent.Type.OBJECT_MODIFIED);
        globalObjectPool.emitObjectEvent(vehicle.clone(), previousVehicle, TCSObjectEvent.Type.OBJECT_MODIFIED);
        globalObjectPool.emitObjectEvent(currentBin, previousBin, TCSObjectEvent.Type.OBJECT_MODIFIED);
        dataBaseService.updateDataBase();
      }
      else{
        LOG.error("ERROR {} droping bin to {} failed",vehicleRef.getName(),location.getName());
        vehicle = globalObjectPool.replaceObject(vehicle.withBin(new Bin("")));
        globalObjectPool.emitObjectEvent(vehicle.clone(), previousVehicle, TCSObjectEvent.Type.OBJECT_MODIFIED);
      }
    }
  }
    
  @Override
  public List<TCSObject<?>> getDestinations(List<TCSObjectReference<?>> opDestinations){
    List<TCSObject<?>> destinations = new ArrayList<>();
    for(TCSObjectReference<?> opDestination:opDestinations){
      TCSObject<?> destination = globalObjectPool.getObjectOrNull(opDestination);
      destinations.add(destination);
    }
    return destinations;
  }
    
  private Bin updateBinAfterPick(Vehicle vehicle, Bin previousBin) {
    TransportOrderBin currTOB = globalObjectPool
        .getObject(TransportOrderBin.class,
                   globalObjectPool.getObject(TransportOrder.class,
                                              vehicle.getTransportOrder()).getAttachedTOrderBin());
    Map<String,Integer> requiredSkus = currTOB.getRequiredSku();
    
    Set<Bin.SKU> updatedSkus = new HashSet<>();
    previousBin.getSKUs().forEach(p -> {
      Integer quantity = requiredSkus.get(p.getSkuID());
      if(quantity != null)
        p = new Bin.SKU(p.getSkuID(), p.getQuantity() - quantity);
      if(p.getQuantity() > 0)
        updatedSkus.add(p);
    });
    return previousBin.withSKUs(updatedSkus).unlock();
  }
  /////////////////////////////////////////////////////////////////////////////////////////////modified end

}
