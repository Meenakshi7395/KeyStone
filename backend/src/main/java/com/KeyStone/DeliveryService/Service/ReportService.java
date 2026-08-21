package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Report.ReportSummaryResponseDTO;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    public ReportService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }


    @Transactional(readOnly = true)
    public ReportSummaryResponseDTO getSummary() {

        long totalWorkOrders =
                workOrderRepository.count();

        long openWorkOrders =
                workOrderRepository.countByStatus(
                        WorkOrderStatus.OPEN
                );

        long assignedWorkOrders =
                workOrderRepository.countByStatus(
                        WorkOrderStatus.ASSIGNED
                );

        long inProgressWorkOrders =
                workOrderRepository.countByStatus(
                        WorkOrderStatus.IN_PROGRESS
                );

        long completedWorkOrders =
                workOrderRepository.countByStatus(
                        WorkOrderStatus.COMPLETED
                );

        long totalCustomers =
                customerRepository.count();

        long totalTechnicians =
                userRepository.countByRole(
                        Role.TECHNICIAN
                );

        return new ReportSummaryResponseDTO(
                totalWorkOrders,
                openWorkOrders,
                assignedWorkOrders,
                inProgressWorkOrders,
                completedWorkOrders,
                totalCustomers,
                totalTechnicians
        );
    }
}
