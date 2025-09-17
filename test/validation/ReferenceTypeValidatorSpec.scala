/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package validation

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import models.DirectDebitSource.*

class ReferenceTypeValidatorSpec extends AnyFreeSpec with Matchers {
  
  "ReferenceType Validator" - {
    "must validate PAYE reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[PAYE.type]]
      val validPAYEReference = Seq("961PX0023480X", "861PV00002023", "N961PX0023480X", "961PX0023480X")
      validPAYEReference.foreach(
        ref =>
          validation.validate(ref) mustEqual true
      )
    }

    "must fail to validate PAYE reference with incorrect format" in {
      val validation = summon[ReferenceTypeValidator.Validator[PAYE.type]]
      val invalidPAYEReference = Seq("A961PX0023480X", "ABCPD0020230X", "861PH0020230X", "961PX0Y234809", "961PX1023480X")

      invalidPAYEReference.foreach(
        ref =>
          validation.validate(ref) mustEqual false
      )
    }

    "must validate MGD reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[MGD.type]]
      val validMGDReference = "XVM00005554321"
      validation.validate(validMGDReference) mustEqual true
    }

    "must fail to validate MGD reference with incorrect format" in {
      val validation = summon[ReferenceTypeValidator.Validator[MGD.type]]
      val invalidMGDReference = Seq("XBM00005554321", "XVX00005554321", "XVM01005554321")

      invalidMGDReference.foreach(
        ref =>
          validation.validate(ref) mustEqual false
      )
    }

    "must validate SA reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[SA.type]]
      val validSAReference = Seq("5829820384K", "5975326728K", "6577919948K", "4091324863K",
        "5028722210K", "5259227426K", "5614425520K", "5882918939K", "6131803582K", "7406205697K",
        "7508225717K", "7638108392K", "8194304206K", "8278617246K", "8737823777K", "8337003895K",
        "8337004125K", "8337005582K", "8337006764K", "8337006814K", "8337007400K", "8337010696K",
        "8337011413K", "8337011525K", "8337012240K", "8337013386K", "8337015056K", "8337018233K",
        "8337018250K", "8337018362K", "8337018376K", "8337018412K", "8337018507K")
      validSAReference.foreach(
        ref => validation.validate(ref) mustEqual true
      )
    }

    "must fail to validate SA reference with incorrect format" in {
      val validation = summon[ReferenceTypeValidator.Validator[SA.type]]
      val invalidSAReference = Seq("1234567890K", "5829820384Z", "5829820384K ", "5829820384K ", " 5829820384K",
        "1829820384K", "5829830384K")
      invalidSAReference.foreach(
        ref => validation.validate(ref) mustEqual false
      )
    }

    "must validate CT reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[CT.type]]
      Seq("5829820384A001", "5975326728A001", "6577919948A001", "4091324863A001",
        "5028722210A001", "5259227426A001", "5614425520A001", "5882918939A001", "6131803582A001",
        "7406205697A001", "7508225717A001", "7638108392A001", "8194304206A001", "8278617246A001",
        "8737823777A001", "8337003895A001", "8337004125A001", "8337005582A001", "8337006764A001",
        "8337006814A001", "8337007400A001", "8337010696A001", "8337011413A001", "8337011525A001",
        "8337012240A001", "8337013386A001", "8337015056A001", "8337018233A001", "8337018250A001",
        "8337018362A001", "8337018376A001", "8337018412A001", "8337018507A001"
      ).map{
         prefix =>
          (0 until 100).map{
          i =>
            val variedDigits = if (i < 10) "0" + i else String.valueOf(i)
            val testVal = prefix + variedDigits + "A"
            validation.validate(testVal) mustEqual true
        }
      }
    }

    "must fail to validate CT reference with incorrect format " in {
       val validation = summon[ReferenceTypeValidator.Validator[CT.type]]
       val invalidCTReference = Seq("9111111112A00108A", "1111111112X00108A", "9111111112AX0108A", "9111111112A0X108A",
         "9111111112A00X08A", "9111111112A001X8A", "9111111112A0010XA", "9111111112A00108X")

       invalidCTReference.foreach(
         ref => validation.validate(ref) mustEqual false
       )
     }

    "must validate SDLT reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[SDLT.type]]
      val validSDLTReference = Seq("100000511MX", "100000504ML", "100000508MT", "100000513MM", "100000523MP")

      validSDLTReference.foreach(
        ref => validation.validate(ref) mustEqual true
      )
    }

    "must fail to validate SDLT reference with incorrect format " in {
      val validation = summon[ReferenceTypeValidator.Validator[SDLT.type]]
      val validSDLTReference = Seq("100000511MK", "100021504ML", "100000509MT", "100000523MM", "110000523MP")

      validSDLTReference.foreach(
        ref => validation.validate(ref) mustEqual false
      )
    }

    "must validate VAT reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[VAT.type]]
      val validVATReference = Seq("562235945", "100212755", "959939732", "562235987", "232457280", "627076730", "754843112",
        "870484115", "917876084", "722786517", "220430231")

      validVATReference.foreach(
        ref => validation.validate(ref) mustEqual true
      )
    }

    "must invalidate VAT reference with incorrect format" in {
      val validation = summon[ReferenceTypeValidator.Validator[VAT.type]]
      val invalidVATReference = Seq("562235946", "100212756", "959939832", "562235988", "232457281", "627076740", "754843122",
        "870584115", "917876085", "722786518", "220430232")

      invalidVATReference.foreach(
        ref => validation.validate(ref) mustEqual false
      )
    }

    "must validate NIC reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[NIC.type]]
      val validNICReference = Seq("600340016213526259", "600340016213536354", "601340016113236351", "601340014200236351",
        "601340010200236358")

      validNICReference.foreach(
        ref => validation.validate(ref) mustEqual true
      )
    }

    "must fail to validate NIC reference with incorrect format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[NIC.type]]
      val invalidNICReference = Seq("600340113213526259", "600340016213536357", "601340016113236451", "601340014200236451",
        "601340010200234358")

      invalidNICReference.foreach(
        ref => validation.validate(ref) mustEqual false
      )
    }

    "must validate Other Liability reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[OL.type]]
      val validOLReference = Seq(
        "XW200001000187", "XG000001000188", "XS000001000191", "XD000001000195", "XX100001000203", "XN900001000204",
        "XZN10001100205", "XRZ10001100555", "XBN10001100555", "XKK10001100550", "XZK10001100555", "XXX10001110484",
        "XR0000100000552", "XV0000100000556", "XA0000100000558", "XZ0000100000563", "XB0000100000567"
      )

      validOLReference.foreach(
        ref => validation.validate(ref) mustEqual true
      )
    }

    "must fail to validate Other Liability reference with incorrect format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[OL.type]]
      val invalidOLReference = Seq(
        "XE000001000189", "XG000001000189", "XS000001000193", "XD000001000196", "XW000001000204", "XB000001000205",
        "PH000001000205", "9H000001000205", "XRM00001000205", "XDNA0001000205", "XDN0000100020Z", "XDN00001P00205",
        "XR0000100000554", "XV0000100000559", "XA0000100000556", "XZ0000100000583", "XB0000100000767",
        "XAM000100000767", "PH0000100000767", "9H0000100000767", "XH000010000076Z", "XB000010H000761"
      )

      invalidOLReference.foreach(
        ref => validation.validate(ref) mustEqual false
      )
    }

    "must validate Tax Credit reference with correct format successfully" in {
      val validation = summon[ReferenceTypeValidator.Validator[TC.type]]
      val validTaxCreditReference = "WT447571311207NE"

      validation.validate(validTaxCreditReference) mustEqual true
    }

    "must fail to validate Tax Credit reference with incorrect format" in {
      val validation = summon[ReferenceTypeValidator.Validator[TC.type]]
      val invalidTaxCreditReference = Seq("AB123456310724NC", "ZT23456280724NC")

      invalidTaxCreditReference.foreach(
        ref => validation.validate(ref) mustEqual false
      )
    }
  }

}
