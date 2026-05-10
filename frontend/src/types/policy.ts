export interface PolInfoEntity {
  id: number;
  polNo: string;
  policyHolderName: string;
  insuredName: string;
  idCardNo: string;
  insuranceCompany: string;
  productName: string;
  insuranceType: string;
  premiumAmount: number;
  insuredAmount: number;
  status: string;
  effectiveDate: string;
  expiryDate: string;
  createTime: string;
  updateTime: string;
}

export interface PolicyQueryRequest {
  polNo?: string;
  policyHolderName?: string;
  idCardNo?: string;
  status?: string;
}
