package com.laralbir.japantraveltools.feature.emergency.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laralbir.japantraveltools.R
import com.laralbir.japantraveltools.feature.emergency.domain.model.MedicalEmergencyProfile

@Composable
fun MedicalProfileSection(
    profile: MedicalEmergencyProfile,
    isSaving: Boolean,
    saveSuccess: Boolean,
    onSaveProfile: (MedicalEmergencyProfile) -> Unit,
    onResetSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var fullName by remember(profile) { mutableStateOf(profile.fullName) }
    var bloodType by remember(profile) { mutableStateOf(profile.bloodType) }
    var allergiesText by remember(profile) { mutableStateOf(profile.allergies.joinToString(", ")) }
    var conditions by remember(profile) { mutableStateOf(profile.conditions) }
    var medications by remember(profile) { mutableStateOf(profile.medications) }

    var insuranceCompany by remember(profile) { mutableStateOf(profile.insuranceCompany) }
    var insurancePolicyNumber by remember(profile) { mutableStateOf(profile.insurancePolicyNumber) }
    var insuranceEmergencyPhone by remember(profile) { mutableStateOf(profile.insuranceEmergencyPhone) }

    var emergencyContactName by remember(profile) { mutableStateOf(profile.emergencyContactName) }
    var emergencyContactPhone by remember(profile) { mutableStateOf(profile.emergencyContactPhone) }
    var emergencyContactRelation by remember(profile) { mutableStateOf(profile.emergencyContactRelation) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(
                context,
                context.getString(R.string.emergency_medical_saved_toast),
                Toast.LENGTH_SHORT
            ).show()
            onResetSaveSuccess()
        }
    }

    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Column(modifier = modifier.fillMaxWidth()) {
        // Section: Personal & Medical Data
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🩺 Datos Médicos y Fisiológicos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text(stringResource(R.string.emergency_medical_fullname)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.emergency_medical_blood_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bloodTypes.forEach { type ->
                        FilterChip(
                            selected = bloodType.equals(type, ignoreCase = true),
                            onClick = { bloodType = type },
                            label = { Text(type, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = allergiesText,
                    onValueChange = { allergiesText = it },
                    label = { Text(stringResource(R.string.emergency_medical_allergies)) },
                    placeholder = { Text("Ej: Penicilina, Cacahuetes, Marisco") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text(stringResource(R.string.emergency_medical_conditions)) },
                    placeholder = { Text("Ej: Asma, Diabetes, Hipertensión") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = medications,
                    onValueChange = { medications = it },
                    label = { Text(stringResource(R.string.emergency_medical_medications)) },
                    placeholder = { Text("Ej: Insulina, Inhalador Salbutamol") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Travel Insurance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "🛡️ Seguro Médico de Viaje",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = insuranceCompany,
                    onValueChange = { insuranceCompany = it },
                    label = { Text(stringResource(R.string.emergency_medical_insurance_company)) },
                    placeholder = { Text("Ej: IATI, Mondo, Chapka, Mapfre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = insurancePolicyNumber,
                    onValueChange = { insurancePolicyNumber = it },
                    label = { Text(stringResource(R.string.emergency_medical_policy_number)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = insuranceEmergencyPhone,
                    onValueChange = { insuranceEmergencyPhone = it },
                    label = { Text(stringResource(R.string.emergency_medical_insurance_phone)) },
                    placeholder = { Text("+34 93 485 77 00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (insuranceEmergencyPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clean = insuranceEmergencyPhone.replace(" ", "")
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_medical_call_insurance),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: Emergency Family Contact
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "👨‍👩‍👧 Contacto Familiar SOS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emergencyContactName,
                    onValueChange = { emergencyContactName = it },
                    label = { Text(stringResource(R.string.emergency_medical_contact_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emergencyContactRelation,
                    onValueChange = { emergencyContactRelation = it },
                    label = { Text(stringResource(R.string.emergency_medical_contact_relation)) },
                    placeholder = { Text("Ej: Cónyuge, Madre, Hermano") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emergencyContactPhone,
                    onValueChange = { emergencyContactPhone = it },
                    label = { Text(stringResource(R.string.emergency_medical_contact_phone)) },
                    placeholder = { Text("+34 600 000 000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                if (emergencyContactPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clean = emergencyContactPhone.replace(" ", "")
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_medical_call_contact),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = {
                val allergiesList = allergiesText
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val updated = MedicalEmergencyProfile(
                    fullName = fullName.trim(),
                    bloodType = bloodType.trim(),
                    allergies = allergiesList,
                    conditions = conditions.trim(),
                    medications = medications.trim(),
                    insuranceCompany = insuranceCompany.trim(),
                    insurancePolicyNumber = insurancePolicyNumber.trim(),
                    insuranceEmergencyPhone = insuranceEmergencyPhone.trim(),
                    emergencyContactName = emergencyContactName.trim(),
                    emergencyContactPhone = emergencyContactPhone.trim(),
                    emergencyContactRelation = emergencyContactRelation.trim()
                )
                onSaveProfile(updated)
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(24.dp).width(24.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.emergency_medical_save_action),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
