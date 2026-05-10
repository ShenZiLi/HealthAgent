import type { Result } from '@/types/auth';
import type {
  ExaminationHospital,
  ExaminationPackage,
  ExaminationPlan,
  ExaminationBooking,
  BookingRequest,
} from '@/types/examination';

const API_BASE = '/api/examinations';

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

export const examinationApi = {
  async getHospitals(keyword?: string): Promise<Result<ExaminationHospital[]>> {
    const url = keyword
      ? `${API_BASE}/hospitals?keyword=${encodeURIComponent(keyword)}`
      : `${API_BASE}/hospitals`;
    return fetchRequest<ExaminationHospital[]>(url, { method: 'GET' });
  },

  async getPackages(): Promise<Result<ExaminationPackage[]>> {
    return fetchRequest<ExaminationPackage[]>(`${API_BASE}/packages`, { method: 'GET' });
  },

  async getPlans(params?: {
    hospitalId?: number;
    packageId?: number;
    startDate?: string;
    endDate?: string;
  }): Promise<Result<ExaminationPlan[]>> {
    const query = new URLSearchParams();
    if (params?.hospitalId) query.set('hospitalId', params.hospitalId.toString());
    if (params?.packageId) query.set('packageId', params.packageId.toString());
    if (params?.startDate) query.set('startDate', params.startDate);
    if (params?.endDate) query.set('endDate', params.endDate);
    const qs = query.toString();
    return fetchRequest<ExaminationPlan[]>(`${API_BASE}/plans${qs ? '?' + qs : ''}`, {
      method: 'GET',
    });
  },

  async bookExamination(request: BookingRequest): Promise<Result<ExaminationBooking>> {
    return fetchRequest<ExaminationBooking>(`${API_BASE}/book`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },

  async getUserBookings(userId: string): Promise<Result<ExaminationBooking[]>> {
    return fetchRequest<ExaminationBooking[]>(`${API_BASE}/users/${userId}/bookings`, {
      method: 'GET',
    });
  },

  async cancelBooking(bookingNo: string, userId: string): Promise<Result<boolean>> {
    return fetchRequest<boolean>(`${API_BASE}/bookings/${bookingNo}?userId=${userId}`, {
      method: 'DELETE',
    });
  },

  async getRequirements(): Promise<Result<string>> {
    return fetchRequest<string>(`${API_BASE}/requirements`, { method: 'GET' });
  },
};
