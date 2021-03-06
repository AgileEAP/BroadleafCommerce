/*
 * Copyright 2008-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.broadleafcommerce.core.order.domain;

import org.broadleafcommerce.common.currency.util.BroadleafCurrencyUtils;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.presentation.AdminPresentation;
import org.broadleafcommerce.common.presentation.AdminPresentationClass;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.broadleafcommerce.core.catalog.domain.Product;
import org.broadleafcommerce.core.catalog.domain.ProductBundle;
import org.broadleafcommerce.core.catalog.domain.ProductBundleImpl;
import org.broadleafcommerce.core.catalog.domain.Sku;
import org.broadleafcommerce.core.catalog.domain.SkuImpl;
import org.broadleafcommerce.core.catalog.service.type.ProductBundlePricingModelType;
import org.broadleafcommerce.core.order.service.manipulation.OrderItemVisitor;
import org.broadleafcommerce.core.pricing.service.exception.PricingException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "BLC_BUNDLE_ORDER_ITEM")
@Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region="blOrderElements")
@AdminPresentationClass(friendlyName = "BundleOrderItemImpl_bundleOrderItem")
public class BundleOrderItemImpl extends OrderItemImpl implements BundleOrderItem {

    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "bundleOrderItem", targetEntity = DiscreteOrderItemImpl.class, cascade = {CascadeType.ALL})
    @Cache(usage=CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region="blOrderElements")
    protected List<DiscreteOrderItem> discreteOrderItems = new ArrayList<DiscreteOrderItem>();
    
    @OneToMany(mappedBy = "bundleOrderItem", targetEntity = BundleOrderItemFeePriceImpl.class, cascade = { CascadeType.ALL })
    @Cascade(value = { org.hibernate.annotations.CascadeType.ALL, org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "blOrderElements")
    protected List<BundleOrderItemFeePrice> bundleOrderItemFeePrices = new ArrayList<BundleOrderItemFeePrice>();

    @Column(name="BASE_RETAIL_PRICE", precision=19, scale=5)
    @AdminPresentation(friendlyName = "BundleOrderItemImpl_Base_Retail_Price", order=2, group = "BundleOrderItemImpl_Pricing", fieldType= SupportedFieldType.MONEY)
    protected BigDecimal baseRetailPrice;

    @Column(name="BASE_SALE_PRICE", precision=19, scale=5)
    @AdminPresentation(friendlyName = "BundleOrderItemImpl_Base_Sale_Price", order=2, group = "BundleOrderItemImpl_Pricing", fieldType= SupportedFieldType.MONEY)
    protected BigDecimal baseSalePrice;

    @ManyToOne(targetEntity = SkuImpl.class)
    @JoinColumn(name = "SKU_ID")
    @NotFound(action = NotFoundAction.IGNORE)
    protected Sku sku;

    @ManyToOne(targetEntity = ProductBundleImpl.class)
    @JoinColumn(name = "PRODUCT_BUNDLE_ID")
    @AdminPresentation(excluded = true)
    protected ProductBundle productBundle;

    @Override
    public Sku getSku() {
           return sku;
    }

    @Override
    public void setSku(Sku sku) {
       this.sku = sku;
        if (sku != null) {
           if (sku.getRetailPrice() != null) {
               this.baseRetailPrice = sku.getRetailPrice().getAmount();
           }
           if (sku.getSalePrice() != null) {
               this.baseSalePrice = sku.getSalePrice().getAmount();
           }
           this.itemTaxable = sku.isTaxable();
           setName(sku.getName());
        }
    }

    @Override
    public ProductBundle getProductBundle() {
        return productBundle;
    }

    @Override
    public void setProductBundle(ProductBundle productBundle) {
        this.productBundle = productBundle;
    }

    @Override
    public List<DiscreteOrderItem> getDiscreteOrderItems() {
        return discreteOrderItems;
    }

    @Override
    public void setDiscreteOrderItems(List<DiscreteOrderItem> discreteOrderItems) {
        this.discreteOrderItems = discreteOrderItems;
    }

    @Override
    public List<BundleOrderItemFeePrice> getBundleOrderItemFeePrices() {
        return bundleOrderItemFeePrices;
    }

    @Override
    public void setBundleOrderItemFeePrices(List<BundleOrderItemFeePrice> bundleOrderItemFeePrices) {
        this.bundleOrderItemFeePrices = bundleOrderItemFeePrices;
    }

    @Override
    public void removeAllCandidateItemOffers() {
        if (shouldSumItems()) {
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                discreteOrderItem.removeAllCandidateItemOffers();
            }
        } else {
            super.removeAllCandidateItemOffers();
        }
    }

    @Override
    public int removeAllAdjustments() {
        if (shouldSumItems()) {
            int removedAdjustmentCount = 0;
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                removedAdjustmentCount = removedAdjustmentCount + discreteOrderItem.removeAllAdjustments();
            }
            return removedAdjustmentCount;
        } else {
            return super.removeAllAdjustments();
        }
    }

    @Override
    public void assignFinalPrice() {
        if (shouldSumItems()) {
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                discreteOrderItem.assignFinalPrice();
            }
        }
        price = getCurrentPrice().getAmount();
    }

    @Override
    public Money getTaxablePrice() {
        if (shouldSumItems()) {
            Money currentBundleTaxablePrice = BroadleafCurrencyUtils.getMoney(getOrder().getCurrency());
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                BigDecimal currentItemTaxablePrice = discreteOrderItem.getTaxablePrice().getAmount();
                BigDecimal priceWithQuantity = currentItemTaxablePrice.multiply(new BigDecimal(discreteOrderItem.getQuantity()));
                currentBundleTaxablePrice = currentBundleTaxablePrice.add(BroadleafCurrencyUtils.getMoney(priceWithQuantity, getOrder().getCurrency()));
            }
            for (BundleOrderItemFeePrice fee : getBundleOrderItemFeePrices()) {
                if (fee.isTaxable()) {
                    currentBundleTaxablePrice = currentBundleTaxablePrice.add(fee.getAmount());
                }
            }
            return currentBundleTaxablePrice;
        } else {
            Money taxablePrice = BroadleafCurrencyUtils.getMoney(BigDecimal.ZERO, getOrder().getCurrency());
            if (sku != null && sku.isTaxable() == null || sku.isTaxable()) {
                taxablePrice = getPrice();
            }
            return taxablePrice;
        }
    }

    @Override
    public boolean shouldSumItems() {
        if (productBundle != null) {
            return ProductBundlePricingModelType.ITEM_SUM.equals(productBundle.getPricingModel());
        }
        return true;
    }

    @Override
    public Money getRetailPrice() {
        if (shouldSumItems()) {
            Money bundleRetailPrice = BroadleafCurrencyUtils.getMoney(getOrder().getCurrency());
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                BigDecimal itemRetailPrice = discreteOrderItem.getRetailPrice().getAmount();
                BigDecimal quantityPrice = itemRetailPrice.multiply(new BigDecimal(discreteOrderItem.getQuantity()));
                bundleRetailPrice = bundleRetailPrice.add(BroadleafCurrencyUtils.getMoney(quantityPrice, getOrder().getCurrency()));
            }
            for (BundleOrderItemFeePrice fee : getBundleOrderItemFeePrices()) {
                bundleRetailPrice = bundleRetailPrice.add(fee.getAmount());
            }
            return bundleRetailPrice;
        } else {
            return super.getRetailPrice();
        }
    }


    @Override
    public Money getSalePrice() {

        if (shouldSumItems()) {
            Money bundleSalePrice = null;
            if (hasSaleItems()) {
                bundleSalePrice = BroadleafCurrencyUtils.getMoney(getOrder().getCurrency());
                for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                    BigDecimal itemSalePrice = null;
                    if (discreteOrderItem.getSalePrice() != null) {
                        itemSalePrice = discreteOrderItem.getSalePrice().getAmount();
                    } else {
                        itemSalePrice = discreteOrderItem.getRetailPrice().getAmount();
                    }
                    BigDecimal quantityPrice = itemSalePrice.multiply(new BigDecimal(discreteOrderItem.getQuantity()));
                    bundleSalePrice = bundleSalePrice.add(BroadleafCurrencyUtils.getMoney(quantityPrice, getOrder().getCurrency()));
                }
                for (BundleOrderItemFeePrice fee : getBundleOrderItemFeePrices()) {
                    bundleSalePrice = bundleSalePrice.add(fee.getAmount());
                }
            }
            return bundleSalePrice;
        } else {
            return super.getSalePrice();
        }
    }

    @Override
    public Money getBaseRetailPrice() {
        return convertToMoney(baseRetailPrice);
    }

    @Override
    public void setBaseRetailPrice(Money baseRetailPrice) {
        this.baseRetailPrice = baseRetailPrice==null?null:baseRetailPrice.getAmount();
    }

    @Override
    public Money getBaseSalePrice() {
        return convertToMoney(baseSalePrice);
    }

    @Override
    public void setBaseSalePrice(Money baseSalePrice) {
        this.baseSalePrice = baseSalePrice==null?null:baseSalePrice.getAmount();
    }

    private boolean hasSaleItems() {
        for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
            if (discreteOrderItem.getSalePrice() != null) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean hasAdjustedItems() {
        for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
            if (discreteOrderItem.getAdjustmentValue().greaterThan(BroadleafCurrencyUtils.getMoney(BigDecimal.ZERO, getOrder().getCurrency()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Money getCurrentPrice() {
        if (shouldSumItems()) {
            Money currentBundlePrice = BroadleafCurrencyUtils.getMoney(getOrder().getCurrency());
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                BigDecimal currentItemPrice = discreteOrderItem.getCurrentPrice().getAmount();
                BigDecimal quantityPrice = currentItemPrice.multiply(new BigDecimal(discreteOrderItem.getQuantity()));
                currentBundlePrice = currentBundlePrice.add(BroadleafCurrencyUtils.getMoney(quantityPrice, getOrder().getCurrency()));
            }
            return currentBundlePrice;
        } else {
            return super.getCurrentPrice();
        }
    }
    
    @Override
    public boolean updatePrices() {
        boolean updated = false;

        // Only need to update prices if we are not summing the contained items to determine
        // the price.
        if (! shouldSumItems()) {
            if (getSku() != null && !getSku().getRetailPrice().equals(getRetailPrice())) {
                setBaseRetailPrice(getSku().getRetailPrice());
                setRetailPrice(getSku().getRetailPrice());
                updated = true;
            }
            if (getSku() != null && getSku().getSalePrice() != null && !getSku().getSalePrice().equals(getSalePrice())) {
                setBaseSalePrice(getSku().getSalePrice());
                setSalePrice(getSku().getSalePrice());
                updated = true;
            }
        }
        return updated;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BundleOrderItemImpl other = (BundleOrderItemImpl) obj;
        
        if (!super.equals(obj)) {
            return false;
        }

        if (id != null && other.id != null) {
            return id.equals(other.id);
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public Product getProduct() {
        return getProductBundle();
    }

    protected Money convertToMoney(BigDecimal amount) {
        return amount == null ? null : BroadleafCurrencyUtils.getMoney(amount, getOrder().getCurrency());
    }

    @Override
    public OrderItem clone() {
        BundleOrderItemImpl orderItem = (BundleOrderItemImpl) super.clone();
        if (discreteOrderItems != null) {
            for (DiscreteOrderItem discreteOrderItem : discreteOrderItems) {
                DiscreteOrderItem temp = (DiscreteOrderItem) discreteOrderItem.clone();
                temp.setBundleOrderItem(orderItem);
                orderItem.getDiscreteOrderItems().add(temp);
            }
        }
        if (bundleOrderItemFeePrices != null) {
            for (BundleOrderItemFeePrice feePrice : bundleOrderItemFeePrices) {
                BundleOrderItemFeePrice cloneFeePrice = feePrice.clone();
                cloneFeePrice.setBundleOrderItem(orderItem);
                orderItem.getBundleOrderItemFeePrices().add(cloneFeePrice);
            }
        }

        orderItem.setBaseRetailPrice(convertToMoney(baseRetailPrice));
        orderItem.setBaseSalePrice(convertToMoney(baseSalePrice));
        orderItem.setSku(sku);
        orderItem.setProductBundle(productBundle);

        return orderItem;
    }

    @Override
    public int hashCode() {
        final int prime = super.hashCode();
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
    
    @Override
    public void accept(OrderItemVisitor visitor) throws PricingException {
        visitor.visit(this);
    }
}
