/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amazonaws.services.sagemaker.sparksdk.transformation.serializers

import org.scalatest._
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mock.MockitoSugar

import org.apache.spark.ml.linalg.{DenseVector, SparseVector, SQLDataTypes}
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}

import com.amazonaws.services.sagemaker.sparksdk.transformation.deserializers.LibSVMResponseRowDeserializer

class LibSVMRequestRowSerializerTests extends FlatSpec with Matchers with MockitoSugar {
  val schema = new LibSVMResponseRowDeserializer(10).schema

  "LibSVMRequestRowSerializer" should "serialize sparse vector" in {

    val vec = new SparseVector(100, Seq[Int](0, 10).toArray, Seq[Double](-100.0, 100.1).toArray)
    val row = new GenericRowWithSchema(values = Seq(1.0, vec).toArray, schema = schema)
    val rrs = new LibSVMRequestRowSerializer(Some(schema))
    val serialized = new String(rrs.serializeRow(row))
    assert ("1.0 1:-100.0 11:100.1\n" == serialized)
  }

  it should "serialize dense vector" in {

    val vec = new DenseVector(Seq(10.0, -100.0, 2.0).toArray)
    val row = new GenericRowWithSchema(values = Seq(1.0, vec).toArray, schema = schema)
    val rrs = new LibSVMRequestRowSerializer(Some(schema))
    val serialized = new String(rrs.serializeRow(row))
    assert("1.0 1:10.0 2:-100.0 3:2.0\n" == serialized)
  }

  it should "ignore other columns" in {
    val schemaWithExtraColumns = StructType(Array(
      StructField("name", StringType, nullable = false),
      StructField("label", DoubleType, nullable = false),
      StructField("features", SQLDataTypes.VectorType, nullable = false),
        StructField("favorite activity", StringType, nullable = false)))

    val vec = new DenseVector(Seq(10.0, -100.0, 2.0).toArray)
    val row = new GenericRowWithSchema(values = Seq("Elizabeth", 1.0, vec, "Crying").toArray,
      schema = schemaWithExtraColumns)

    val rrs = new LibSVMRequestRowSerializer(Some(schemaWithExtraColumns))
    val serialized = new String(rrs.serializeRow(row))
    assert("1.0 1:10.0 2:-100.0 3:2.0\n" == serialized)
  }

  it should "fail on invalid features column name" in {
    val vec = new DenseVector(Seq(10.0, -100.0, 2.0).toArray)
    intercept[RuntimeException] {
      new LibSVMRequestRowSerializer(Some(schema), featuresColumnName = "i do not exist dear sir!")
    }
  }

  it should "fail on invalid label column name" in {
    val vec = new DenseVector(Seq(10.0, -100.0, 2.0).toArray)
    intercept[RuntimeException] {
      new LibSVMRequestRowSerializer(Some(schema),
        labelColumnName = "Sir! I must protest! I do not exist!")
    }
  }

  it should "fail on invalid types" in {
    val schemaWithInvalidLabelType = StructType(Array(
      StructField("label", StringType, nullable = false),
      StructField("features", SQLDataTypes.VectorType, nullable = false)))
    intercept[RuntimeException] {
      new LibSVMRequestRowSerializer(Some(schemaWithInvalidLabelType))
    }
    val schemaWithInvalidFeaturesType = StructType(Array(
      StructField("label", DoubleType, nullable = false),
      StructField("features", StringType, nullable = false)))
    intercept[RuntimeException] {
      new LibSVMRequestRowSerializer(Some(schemaWithInvalidFeaturesType))
    }
  }

  it should "validate correct schema" in {
    val validSchema = StructType(Array(
      StructField("label", DoubleType, nullable = false),
      StructField("features", SQLDataTypes.VectorType, nullable = false)))
    new LibSVMRequestRowSerializer(Some(validSchema))
  }
}
