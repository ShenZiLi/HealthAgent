export interface PolInfoEntity {
  id: number;
  userId?: string;
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
  userId?: string;
  polNo?: string;
  policyHolderName?: string;
  idCardNo?: string;
  status?: string;
}
