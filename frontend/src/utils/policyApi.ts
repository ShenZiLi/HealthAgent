import type { PolInfoEntity, PolicyQueryRequest, Result } from '@/types/auth';

const API_BASE = '/api/policy';

async function fetchRequest<T>(url: string, options: RequestInit = {}): Promise<Result<T>> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers as Record<string, string>,
  };

  const token = localStorage.getItem('accessToken');
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`);
  }

  const contentType = response.headers.get('content-type');
  if (!contentType || !contentType.includes('application/json')) {
    const text = await response.text();
    throw new Error(`Expected JSON response, but got: ${text || 'Empty response'}`);
  }

  const text = await response.text();
  if (!text) {
    throw new Error('Empty response body');
  }

  try {
    const data = JSON.parse(text);
    return data;
  } catch (e) {
    throw new Error(`Failed to parse JSON: ${text.substring(0, 100)}...`);
  }
}

export const policyApi = {
  async queryPolicies(request: PolicyQueryRequest): Promise<Result<PolInfoEntity[]>> {
    return fetchRequest<PolInfoEntity[]>(`${API_BASE}/query`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async queryPoliciesPage(request: PolicyQueryRequest, pageNum: number = 1, pageSize: number = 10): Promise<Result<any>> {
    return fetchRequest<any>(`${API_BASE}/page?pageNum=${pageNum}&pageSize=${pageSize}`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async getByPolNo(polNo: string): Promise<Result<PolInfoEntity>> {
    return fetchRequest<PolInfoEntity>(`${API_BASE}/${polNo}`, {
      method: 'GET',
    });
  },

  async getByPolicyHolderName(policyHolderName: string): Promise<Result<PolInfoEntity[]>> {
    return fetchRequest<PolInfoEntity[]>(`${API_BASE}/holder/${policyHolderName}`, {
      method: 'GET',
    });
  },

  async getByIdCardNo(idCardNo: string): Promise<Result<PolInfoEntity[]>> {
    return fetchRequest<PolInfoEntity[]>(`${API_BASE}/idcard/${idCardNo}`, {
      method: 'GET',
    });
  },

  async getAllPolicies(): Promise<Result<PolInfoEntity[]>> {
    return fetchRequest<PolInfoEntity[]>(`${API_BASE}/all`, {
      method: 'GET',
    });
  },

  async createPolicy(entity: PolInfoEntity): Promise<Result<boolean>> {
    return fetchRequest<boolean>(`${API_BASE}`, {
      method: 'POST',
      body: JSON.stringify(entity),
    });
  },

  async updatePolicy(entity: PolInfoEntity): Promise<Result<boolean>> {
    return fetchRequest<boolean>(`${API_BASE}`, {
      method: 'PUT',
      body: JSON.stringify(entity),
    });
  },

  async deleteByPolNo(polNo: string): Promise<Result<boolean>> {
    return fetchRequest<boolean>(`${API_BASE}/${polNo}`, {
      method: 'DELETE',
    });
  },
};
