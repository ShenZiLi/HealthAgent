export interface ExaminationHospital {
  id: number;
  hospitalCode: string;
  hospitalName: string;
  hospitalLevel: string;
  address: string;
  phone: string;
  department: string;
  availableSlots: number;
}

export interface ExaminationPackage {
  id: number;
  packageCode: string;
  packageName: string;
  packageDesc: string;
  price: number;
  duration: string;
}

export interface ExaminationBooking {
  id: number;
  bookingNo: string;
  hospitalId: number;
  packageId: number;
  hospitalName: string;
  packageName: string;
  scheduleDate: string;
  price: number;
  userId: string;
  bookerName: string;
  bookerPhone: string;
  idCardNo: string;
  notes: string;
  status: string;
  createTime: string;
}

export interface BookingRequest {
  userId: string;
  bookerName: string;
  bookerPhone: string;
  idCardNo: string;
  notes: string;
  hospitalId?: number;
  packageId?: number;
  scheduleDate?: string;
}
