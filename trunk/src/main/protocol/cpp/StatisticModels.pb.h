// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: StatisticModels.proto

#ifndef PROTOBUF_StatisticModels_2eproto__INCLUDED
#define PROTOBUF_StatisticModels_2eproto__INCLUDED

#include <string>

#include <google/protobuf/stubs/common.h>

#if GOOGLE_PROTOBUF_VERSION < 2005000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers.  Please update
#error your headers.
#endif
#if 2005000 < GOOGLE_PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers.  Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/message.h>
#include <google/protobuf/repeated_field.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/generated_enum_reflection.h>
#include <google/protobuf/unknown_field_set.h>
// @@protoc_insertion_point(includes)

namespace com {
namespace mservice {
namespace momo {
namespace msg {

// Internal implementation detail -- do not call these.
void  protobuf_AddDesc_StatisticModels_2eproto();
void protobuf_AssignDesc_StatisticModels_2eproto();
void protobuf_ShutdownFile_StatisticModels_2eproto();

class Action;
class GetNumber;
class GetNumberReply;

enum ActionType {
  TRANS = 1,
  TRANS_SUCCESS = 2,
  TRANS_BANK_IN = 3,
  TRANS_BANK_IN_SUCCESS = 4,
  TRANS_BANK_IN_20 = 5,
  TRANS_BANK_IN_20_SUCCESS = 6,
  TRANS_BANK_IN_50 = 7,
  TRANS_BANK_IN_50_SUCCESS = 8,
  TRANS_BANK_IN_100 = 9,
  TRANS_BANK_IN_100_SUCCESS = 10,
  TRANS_BANK_IN_200 = 11,
  TRANS_BANK_IN_200_SUCCESS = 12,
  TRANS_BANK_IN_500 = 13,
  TRANS_BANK_IN_500_SUCCESS = 14,
  TRANS_BANK_IN_MAX = 15,
  TRANS_BANK_IN_MAX_SUCCESS = 16,
  TRANS_BANK_OUT = 17,
  TRANS_BANK_OUT_SUCCESS = 18,
  TRANS_BANK_OUT_20 = 19,
  TRANS_BANK_OUT_20_SUCCESS = 20,
  TRANS_BANK_OUT_50 = 21,
  TRANS_BANK_OUT_50_SUCCESS = 22,
  TRANS_BANK_OUT_100 = 23,
  TRANS_BANK_OUT_100_SUCCESS = 24,
  TRANS_BANK_OUT_200 = 25,
  TRANS_BANK_OUT_200_SUCCESS = 26,
  TRANS_BANK_OUT_500 = 27,
  TRANS_BANK_OUT_500_SUCCESS = 28,
  TRANS_BANK_OUT_MAX = 29,
  TRANS_BANK_OUT_MAX_SUCCESS = 30,
  TRANS_TOP_UP = 31,
  TRANS_TOP_UP_SUCCESS = 32,
  TRANS_TOP_UP_20 = 33,
  TRANS_TOP_UP_20_SUCCESS = 34,
  TRANS_TOP_UP_50 = 35,
  TRANS_TOP_UP_50_SUCCESS = 36,
  TRANS_TOP_UP_100 = 37,
  TRANS_TOP_UP_100_SUCCESS = 38,
  TRANS_TOP_UP_200 = 39,
  TRANS_TOP_UP_200_SUCCESS = 40,
  TRANS_TOP_UP_500 = 41,
  TRANS_TOP_UP_500_SUCCESS = 42,
  TRANS_TOP_UP_MAX = 43,
  TRANS_TOP_UP_MAX_SUCCESS = 44,
  TRANS_TOP_UP_GAME = 45,
  TRANS_TOP_UP_GAME_SUCCESS = 46,
  TRANS_TOP_UP_GAME_20 = 47,
  TRANS_TOP_UP_GAME_20_SUCCESS = 48,
  TRANS_TOP_UP_GAME_50 = 49,
  TRANS_TOP_UP_GAME_50_SUCCESS = 50,
  TRANS_TOP_UP_GAME_100 = 51,
  TRANS_TOP_UP_GAME_100_SUCCESS = 52,
  TRANS_TOP_UP_GAME_200 = 53,
  TRANS_TOP_UP_GAME_200_SUCCESS = 54,
  TRANS_TOP_UP_GAME_500 = 55,
  TRANS_TOP_UP_GAME_500_SUCCESS = 56,
  TRANS_TOP_UP_GAME_MAX = 57,
  TRANS_TOP_UP_GAME_MAX_SUCCESS = 58,
  TRANS_M2C = 59,
  TRANS_M2C_SUCCESS = 60,
  TRANS_M2C_20 = 61,
  TRANS_M2C_20_SUCCESS = 62,
  TRANS_M2C_50 = 63,
  TRANS_M2C_50_SUCCESS = 64,
  TRANS_M2C_100 = 65,
  TRANS_M2C_100_SUCCESS = 66,
  TRANS_M2C_200 = 67,
  TRANS_M2C_200_SUCCESS = 68,
  TRANS_M2C_500 = 69,
  TRANS_M2C_500_SUCCESS = 70,
  TRANS_M2C_MAX = 71,
  TRANS_M2C_MAX_SUCCESS = 72,
  TRANS_M2M = 73,
  TRANS_M2M_SUCCESS = 74,
  TRANS_M2M_20 = 75,
  TRANS_M2M_20_SUCCESS = 76,
  TRANS_M2M_50 = 77,
  TRANS_M2M_50_SUCCESS = 78,
  TRANS_M2M_100 = 79,
  TRANS_M2M_100_SUCCESS = 80,
  TRANS_M2M_200 = 81,
  TRANS_M2M_200_SUCCESS = 82,
  TRANS_M2M_500 = 83,
  TRANS_M2M_500_SUCCESS = 84,
  TRANS_M2M_MAX = 85,
  TRANS_M2M_MAX_SUCCESS = 86,
  TRANS_PAY_ONE_BILL = 87,
  TRANS_PAY_ONE_BILL_SUCCESS = 88,
  TRANS_PAY_ONE_BILL_20 = 89,
  TRANS_PAY_ONE_BILL_20_SUCCESS = 90,
  TRANS_PAY_ONE_BILL_50 = 91,
  TRANS_PAY_ONE_BILL_50_SUCCESS = 92,
  TRANS_PAY_ONE_BILL_100 = 93,
  TRANS_PAY_ONE_BILL_100_SUCCESS = 94,
  TRANS_PAY_ONE_BILL_200 = 95,
  TRANS_PAY_ONE_BILL_200_SUCCESS = 96,
  TRANS_PAY_ONE_BILL_500 = 97,
  TRANS_PAY_ONE_BILL_500_SUCCESS = 98,
  TRANS_PAY_ONE_BILL_MAX = 99,
  TRANS_PAY_ONE_BILL_MAX_SUCCESS = 100,
  TRANS_QUICK_PAYMENT = 101,
  TRANS_QUICK_PAYMENT_SUCCESS = 102,
  TRANS_QUICK_PAYMENT_20 = 103,
  TRANS_QUICK_PAYMENT_20_SUCCESS = 104,
  TRANS_QUICK_PAYMENT_50 = 105,
  TRANS_QUICK_PAYMENT_50_SUCCESS = 106,
  TRANS_QUICK_PAYMENT_100 = 107,
  TRANS_QUICK_PAYMENT_100_SUCCESS = 108,
  TRANS_QUICK_PAYMENT_200 = 109,
  TRANS_QUICK_PAYMENT_200_SUCCESS = 110,
  TRANS_QUICK_PAYMENT_500 = 111,
  TRANS_QUICK_PAYMENT_500_SUCCESS = 112,
  TRANS_QUICK_PAYMENT_MAX = 113,
  TRANS_QUICK_PAYMENT_MAX_SUCCESS = 114,
  TRANS_QUICK_DEPOSIT = 115,
  TRANS_QUICK_DEPOSIT_SUCCESS = 116,
  TRANS_QUICK_DEPOSIT_20 = 117,
  TRANS_QUICK_DEPOSIT_20_SUCCESS = 118,
  TRANS_QUICK_DEPOSIT_50 = 119,
  TRANS_QUICK_DEPOSIT_50_SUCCESS = 120,
  TRANS_QUICK_DEPOSIT_100 = 121,
  TRANS_QUICK_DEPOSIT_100_SUCCESS = 122,
  TRANS_QUICK_DEPOSIT_200 = 123,
  TRANS_QUICK_DEPOSIT_200_SUCCESS = 124,
  TRANS_QUICK_DEPOSIT_500 = 125,
  TRANS_QUICK_DEPOSIT_500_SUCCESS = 126,
  TRANS_QUICK_DEPOSIT_MAX = 127,
  TRANS_QUICK_DEPOSIT_MAX_SUCCESS = 128,
  TRAN_SBANK_NET_TO_MOMO = 129,
  TRAN_SBANK_NET_TO_MOMO_SUCCESS = 130,
  TRAN_SBANK_NET_TO_MOMO_20 = 131,
  TRAN_SBANK_NET_TO_MOMO_20_SUCCESS = 132,
  TRAN_SBANK_NET_TO_MOMO_50 = 133,
  TRAN_SBANK_NET_TO_MOMO_50_SUCCESS = 134,
  TRAN_SBANK_NET_TO_MOMO_100 = 135,
  TRAN_SBANK_NET_TO_MOMO_100_SUCCESS = 136,
  TRAN_SBANK_NET_TO_MOMO_200 = 137,
  TRAN_SBANK_NET_TO_MOMO_200_SUCCESS = 138,
  TRAN_SBANK_NET_TO_MOMO_500 = 139,
  TRAN_SBANK_NET_TO_MOMO_500_SUCCESS = 140,
  TRAN_SBANK_NET_TO_MOMO_MAX = 141,
  TRAN_SBANK_NET_TO_MOMO_MAX_SUCCESS = 142,
  WEB_USER = 143,
  MOBILE_USER = 144
};
bool ActionType_IsValid(int value);
const ActionType ActionType_MIN = TRANS;
const ActionType ActionType_MAX = MOBILE_USER;
const int ActionType_ARRAYSIZE = ActionType_MAX + 1;

const ::google::protobuf::EnumDescriptor* ActionType_descriptor();
inline const ::std::string& ActionType_Name(ActionType value) {
  return ::google::protobuf::internal::NameOfEnum(
    ActionType_descriptor(), value);
}
inline bool ActionType_Parse(
    const ::std::string& name, ActionType* value) {
  return ::google::protobuf::internal::ParseNamedEnum<ActionType>(
    ActionType_descriptor(), name, value);
}
// ===================================================================

class Action : public ::google::protobuf::Message {
 public:
  Action();
  virtual ~Action();

  Action(const Action& from);

  inline Action& operator=(const Action& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::google::protobuf::UnknownFieldSet& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::google::protobuf::UnknownFieldSet* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const ::google::protobuf::Descriptor* descriptor();
  static const Action& default_instance();

  void Swap(Action* other);

  // implements Message ----------------------------------------------

  Action* New() const;
  void CopyFrom(const ::google::protobuf::Message& from);
  void MergeFrom(const ::google::protobuf::Message& from);
  void CopyFrom(const Action& from);
  void MergeFrom(const Action& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  ::google::protobuf::uint8* SerializeWithCachedSizesToArray(::google::protobuf::uint8* output) const;
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:

  ::google::protobuf::Metadata GetMetadata() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required .com.mservice.momo.msg.ActionType type = 1;
  inline bool has_type() const;
  inline void clear_type();
  static const int kTypeFieldNumber = 1;
  inline ::com::mservice::momo::msg::ActionType type() const;
  inline void set_type(::com::mservice::momo::msg::ActionType value);

  // optional uint32 number = 2;
  inline bool has_number() const;
  inline void clear_number();
  static const int kNumberFieldNumber = 2;
  inline ::google::protobuf::uint32 number() const;
  inline void set_number(::google::protobuf::uint32 value);

  // @@protoc_insertion_point(class_scope:com.mservice.momo.msg.Action)
 private:
  inline void set_has_type();
  inline void clear_has_type();
  inline void set_has_number();
  inline void clear_has_number();

  ::google::protobuf::UnknownFieldSet _unknown_fields_;

  int type_;
  ::google::protobuf::uint32 number_;

  mutable int _cached_size_;
  ::google::protobuf::uint32 _has_bits_[(2 + 31) / 32];

  friend void  protobuf_AddDesc_StatisticModels_2eproto();
  friend void protobuf_AssignDesc_StatisticModels_2eproto();
  friend void protobuf_ShutdownFile_StatisticModels_2eproto();

  void InitAsDefaultInstance();
  static Action* default_instance_;
};
// -------------------------------------------------------------------

class GetNumber : public ::google::protobuf::Message {
 public:
  GetNumber();
  virtual ~GetNumber();

  GetNumber(const GetNumber& from);

  inline GetNumber& operator=(const GetNumber& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::google::protobuf::UnknownFieldSet& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::google::protobuf::UnknownFieldSet* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const ::google::protobuf::Descriptor* descriptor();
  static const GetNumber& default_instance();

  void Swap(GetNumber* other);

  // implements Message ----------------------------------------------

  GetNumber* New() const;
  void CopyFrom(const ::google::protobuf::Message& from);
  void MergeFrom(const ::google::protobuf::Message& from);
  void CopyFrom(const GetNumber& from);
  void MergeFrom(const GetNumber& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  ::google::protobuf::uint8* SerializeWithCachedSizesToArray(::google::protobuf::uint8* output) const;
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:

  ::google::protobuf::Metadata GetMetadata() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required .com.mservice.momo.msg.ActionType type = 1;
  inline bool has_type() const;
  inline void clear_type();
  static const int kTypeFieldNumber = 1;
  inline ::com::mservice::momo::msg::ActionType type() const;
  inline void set_type(::com::mservice::momo::msg::ActionType value);

  // optional uint64 startDate = 2;
  inline bool has_startdate() const;
  inline void clear_startdate();
  static const int kStartDateFieldNumber = 2;
  inline ::google::protobuf::uint64 startdate() const;
  inline void set_startdate(::google::protobuf::uint64 value);

  // optional uint64 endDate = 3;
  inline bool has_enddate() const;
  inline void clear_enddate();
  static const int kEndDateFieldNumber = 3;
  inline ::google::protobuf::uint64 enddate() const;
  inline void set_enddate(::google::protobuf::uint64 value);

  // @@protoc_insertion_point(class_scope:com.mservice.momo.msg.GetNumber)
 private:
  inline void set_has_type();
  inline void clear_has_type();
  inline void set_has_startdate();
  inline void clear_has_startdate();
  inline void set_has_enddate();
  inline void clear_has_enddate();

  ::google::protobuf::UnknownFieldSet _unknown_fields_;

  ::google::protobuf::uint64 startdate_;
  ::google::protobuf::uint64 enddate_;
  int type_;

  mutable int _cached_size_;
  ::google::protobuf::uint32 _has_bits_[(3 + 31) / 32];

  friend void  protobuf_AddDesc_StatisticModels_2eproto();
  friend void protobuf_AssignDesc_StatisticModels_2eproto();
  friend void protobuf_ShutdownFile_StatisticModels_2eproto();

  void InitAsDefaultInstance();
  static GetNumber* default_instance_;
};
// -------------------------------------------------------------------

class GetNumberReply : public ::google::protobuf::Message {
 public:
  GetNumberReply();
  virtual ~GetNumberReply();

  GetNumberReply(const GetNumberReply& from);

  inline GetNumberReply& operator=(const GetNumberReply& from) {
    CopyFrom(from);
    return *this;
  }

  inline const ::google::protobuf::UnknownFieldSet& unknown_fields() const {
    return _unknown_fields_;
  }

  inline ::google::protobuf::UnknownFieldSet* mutable_unknown_fields() {
    return &_unknown_fields_;
  }

  static const ::google::protobuf::Descriptor* descriptor();
  static const GetNumberReply& default_instance();

  void Swap(GetNumberReply* other);

  // implements Message ----------------------------------------------

  GetNumberReply* New() const;
  void CopyFrom(const ::google::protobuf::Message& from);
  void MergeFrom(const ::google::protobuf::Message& from);
  void CopyFrom(const GetNumberReply& from);
  void MergeFrom(const GetNumberReply& from);
  void Clear();
  bool IsInitialized() const;

  int ByteSize() const;
  bool MergePartialFromCodedStream(
      ::google::protobuf::io::CodedInputStream* input);
  void SerializeWithCachedSizes(
      ::google::protobuf::io::CodedOutputStream* output) const;
  ::google::protobuf::uint8* SerializeWithCachedSizesToArray(::google::protobuf::uint8* output) const;
  int GetCachedSize() const { return _cached_size_; }
  private:
  void SharedCtor();
  void SharedDtor();
  void SetCachedSize(int size) const;
  public:

  ::google::protobuf::Metadata GetMetadata() const;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  // required .com.mservice.momo.msg.ActionType type = 1;
  inline bool has_type() const;
  inline void clear_type();
  static const int kTypeFieldNumber = 1;
  inline ::com::mservice::momo::msg::ActionType type() const;
  inline void set_type(::com::mservice::momo::msg::ActionType value);

  // optional uint64 number = 2;
  inline bool has_number() const;
  inline void clear_number();
  static const int kNumberFieldNumber = 2;
  inline ::google::protobuf::uint64 number() const;
  inline void set_number(::google::protobuf::uint64 value);

  // @@protoc_insertion_point(class_scope:com.mservice.momo.msg.GetNumberReply)
 private:
  inline void set_has_type();
  inline void clear_has_type();
  inline void set_has_number();
  inline void clear_has_number();

  ::google::protobuf::UnknownFieldSet _unknown_fields_;

  ::google::protobuf::uint64 number_;
  int type_;

  mutable int _cached_size_;
  ::google::protobuf::uint32 _has_bits_[(2 + 31) / 32];

  friend void  protobuf_AddDesc_StatisticModels_2eproto();
  friend void protobuf_AssignDesc_StatisticModels_2eproto();
  friend void protobuf_ShutdownFile_StatisticModels_2eproto();

  void InitAsDefaultInstance();
  static GetNumberReply* default_instance_;
};
// ===================================================================


// ===================================================================

// Action

// required .com.mservice.momo.msg.ActionType type = 1;
inline bool Action::has_type() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void Action::set_has_type() {
  _has_bits_[0] |= 0x00000001u;
}
inline void Action::clear_has_type() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void Action::clear_type() {
  type_ = 1;
  clear_has_type();
}
inline ::com::mservice::momo::msg::ActionType Action::type() const {
  return static_cast< ::com::mservice::momo::msg::ActionType >(type_);
}
inline void Action::set_type(::com::mservice::momo::msg::ActionType value) {
  assert(::com::mservice::momo::msg::ActionType_IsValid(value));
  set_has_type();
  type_ = value;
}

// optional uint32 number = 2;
inline bool Action::has_number() const {
  return (_has_bits_[0] & 0x00000002u) != 0;
}
inline void Action::set_has_number() {
  _has_bits_[0] |= 0x00000002u;
}
inline void Action::clear_has_number() {
  _has_bits_[0] &= ~0x00000002u;
}
inline void Action::clear_number() {
  number_ = 0u;
  clear_has_number();
}
inline ::google::protobuf::uint32 Action::number() const {
  return number_;
}
inline void Action::set_number(::google::protobuf::uint32 value) {
  set_has_number();
  number_ = value;
}

// -------------------------------------------------------------------

// GetNumber

// required .com.mservice.momo.msg.ActionType type = 1;
inline bool GetNumber::has_type() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void GetNumber::set_has_type() {
  _has_bits_[0] |= 0x00000001u;
}
inline void GetNumber::clear_has_type() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void GetNumber::clear_type() {
  type_ = 1;
  clear_has_type();
}
inline ::com::mservice::momo::msg::ActionType GetNumber::type() const {
  return static_cast< ::com::mservice::momo::msg::ActionType >(type_);
}
inline void GetNumber::set_type(::com::mservice::momo::msg::ActionType value) {
  assert(::com::mservice::momo::msg::ActionType_IsValid(value));
  set_has_type();
  type_ = value;
}

// optional uint64 startDate = 2;
inline bool GetNumber::has_startdate() const {
  return (_has_bits_[0] & 0x00000002u) != 0;
}
inline void GetNumber::set_has_startdate() {
  _has_bits_[0] |= 0x00000002u;
}
inline void GetNumber::clear_has_startdate() {
  _has_bits_[0] &= ~0x00000002u;
}
inline void GetNumber::clear_startdate() {
  startdate_ = GOOGLE_ULONGLONG(0);
  clear_has_startdate();
}
inline ::google::protobuf::uint64 GetNumber::startdate() const {
  return startdate_;
}
inline void GetNumber::set_startdate(::google::protobuf::uint64 value) {
  set_has_startdate();
  startdate_ = value;
}

// optional uint64 endDate = 3;
inline bool GetNumber::has_enddate() const {
  return (_has_bits_[0] & 0x00000004u) != 0;
}
inline void GetNumber::set_has_enddate() {
  _has_bits_[0] |= 0x00000004u;
}
inline void GetNumber::clear_has_enddate() {
  _has_bits_[0] &= ~0x00000004u;
}
inline void GetNumber::clear_enddate() {
  enddate_ = GOOGLE_ULONGLONG(0);
  clear_has_enddate();
}
inline ::google::protobuf::uint64 GetNumber::enddate() const {
  return enddate_;
}
inline void GetNumber::set_enddate(::google::protobuf::uint64 value) {
  set_has_enddate();
  enddate_ = value;
}

// -------------------------------------------------------------------

// GetNumberReply

// required .com.mservice.momo.msg.ActionType type = 1;
inline bool GetNumberReply::has_type() const {
  return (_has_bits_[0] & 0x00000001u) != 0;
}
inline void GetNumberReply::set_has_type() {
  _has_bits_[0] |= 0x00000001u;
}
inline void GetNumberReply::clear_has_type() {
  _has_bits_[0] &= ~0x00000001u;
}
inline void GetNumberReply::clear_type() {
  type_ = 1;
  clear_has_type();
}
inline ::com::mservice::momo::msg::ActionType GetNumberReply::type() const {
  return static_cast< ::com::mservice::momo::msg::ActionType >(type_);
}
inline void GetNumberReply::set_type(::com::mservice::momo::msg::ActionType value) {
  assert(::com::mservice::momo::msg::ActionType_IsValid(value));
  set_has_type();
  type_ = value;
}

// optional uint64 number = 2;
inline bool GetNumberReply::has_number() const {
  return (_has_bits_[0] & 0x00000002u) != 0;
}
inline void GetNumberReply::set_has_number() {
  _has_bits_[0] |= 0x00000002u;
}
inline void GetNumberReply::clear_has_number() {
  _has_bits_[0] &= ~0x00000002u;
}
inline void GetNumberReply::clear_number() {
  number_ = GOOGLE_ULONGLONG(0);
  clear_has_number();
}
inline ::google::protobuf::uint64 GetNumberReply::number() const {
  return number_;
}
inline void GetNumberReply::set_number(::google::protobuf::uint64 value) {
  set_has_number();
  number_ = value;
}


// @@protoc_insertion_point(namespace_scope)

}  // namespace msg
}  // namespace momo
}  // namespace mservice
}  // namespace com

#ifndef SWIG
namespace google {
namespace protobuf {

template <>
inline const EnumDescriptor* GetEnumDescriptor< ::com::mservice::momo::msg::ActionType>() {
  return ::com::mservice::momo::msg::ActionType_descriptor();
}

}  // namespace google
}  // namespace protobuf
#endif  // SWIG

// @@protoc_insertion_point(global_scope)

#endif  // PROTOBUF_StatisticModels_2eproto__INCLUDED
