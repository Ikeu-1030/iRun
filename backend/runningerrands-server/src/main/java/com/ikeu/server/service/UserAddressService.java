package com.ikeu.server.service;

import com.ikeu.common.exception.NotFoundException;
import com.ikeu.model.dto.AddressSaveDTO;
import com.ikeu.model.entity.UserAddress;
import com.ikeu.model.vo.AddressVO;

import java.util.List;

/**
 * 用户地址簿服务接口，提供地址的增删改查和默认地址设置功能。
 *
 * @author ikeu
 * @since 2026/05/14
 */
public interface UserAddressService {

    /**
     * 新增地址，关联到当前用户。
     *
     * <p>如设置为默认地址，则先清除当前用户其他地址的默认标记。
     *
     * @param userId 用户 ID
     * @param addressSaveDTO 新增地址 DTO（包含联系人、电话、地址详情、经纬度等）
     */
    void saveAddressInfo(Long userId, AddressSaveDTO addressSaveDTO);

    /**
     * 删除指定地址，校验地址存在且属于当前用户后物理删除。
     *
     * @param userId 用户 ID
     * @param addressId 地址 ID
     * @throws NotFoundException 地址不存在或不属于当前用户时抛出
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 修改指定地址信息，校验地址存在且属于当前用户。
     *
     * @param userId 用户 ID
     * @param addressId 地址 ID
     * @param dto 修改地址 DTO（包含联系人、电话、地址详情、经纬度等）
     * @throws NotFoundException 地址不存在或不属于当前用户时抛出
     */
    void updateAddress(Long userId, Long addressId, AddressSaveDTO dto);

    /**
     * 获取当前用户的所有地址列表，默认地址排前，按 ID 倒序排列。
     *
     * @param userId 用户 ID
     * @return 地址 VO 列表
     */
    List<AddressVO> listAddresses(Long userId);

    /**
     * 设置默认地址，先清除当前用户所有地址的默认标记，再将目标地址设为默认。
     *
     * @param userId 用户 ID
     * @param addressId 地址 ID
     * @throws NotFoundException 地址不存在或不属于当前用户时抛出
     */
    void setDefault(Long userId, Long addressId);
}
