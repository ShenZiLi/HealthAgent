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

export interface ExaminationPlan {
  id: number;
  hospitalId: number;
  packageId: number;
  planName: string;
  hospitalName: string;
  packageName: string;
  scheduleDate: string;
  scheduleTime: string;
  totalSlots: number;
  availableSlots: number;
  price: number;
  status: number;
}

export interface ExaminationBooking {
  id: number;
  bookingNo: string;
  planId: number;
  planName: string;
  hospitalName: string;
  packageName: string;
  scheduleDate: string;
  scheduleTime: string;
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
  planId: number;
  userId: string;
  bookerName: string;
  bookerPhone: string;
  idCardNo: string;
  notes: string;
}
