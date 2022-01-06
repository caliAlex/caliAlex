#version 430

//Regular code. should have
//Shadows and bump

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec2 tc;
in vec4 shadow_coord;
in vec3 originalVertex;
in vec3 vertEyeSpacePos;

out vec4 fragColor;

struct PositionalLight
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	vec3 position;
};

struct Material
{	vec4 ambient;
	vec4 diffuse;
	vec4 specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform int shaderConditional;

//Transparency Uniforms
uniform float alpha;
uniform float flipNormal;

layout (binding=0) uniform sampler2D samp;
layout (binding=1) uniform sampler2DShadow shadowTex;
layout (binding=2) uniform sampler2D height;
layout (binding=3) uniform sampler3D noiseUniform;

void main(void)
{ // normalize the light, normal, and view vectors:
	// halfway vector varyingHalfVector was computed in the vertex shader,
	// and interpolated prior to reaching the fragment shader.
	// It is copied into variable H here for convenience later.
	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-varyingVertPos);
	vec3 H = normalize(varyingHalfVector);

	//FOG Stuff
	vec4 fog = vec4(0.7, 0.8, 0.9, 1.0);	//Color
	float fogStart = 0.3;
	float fogEnd = 900;
	float dist = length(vertEyeSpacePos.xyz);
	float fogFactor = clamp(((fogEnd-dist)/(fogEnd - fogStart)), 0.0, 1.0);

	//Add to perturb incoming normal vector
	//vec3 r = -reflect(normalize(-varyingVertPos), normalize(varyingNormal)); //This is for EM
	// controls depth of bumps
	float a = 0.3;

	// controls width of bumps
	float b = 35.0;
	float x = originalVertex.x;
	float y = originalVertex.y;
	float z = originalVertex.z;

	//Returns 0 or 1 indicating whether pixel is in shadow
	float notInShadow = textureProj(shadowTex, shadow_coord);

	// get the angle between the light and surface normal:
	float cosTheta = dot(L, N);


	// get angle between the normal and the halfway vector
	float cosPhi = dot(H, N);

	// compute ADS contributions (per pixel):
	vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
	vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta, 0.0);
	vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi, 0.0), material.shininess*3.0);


	//No bump mapping
	if (shaderConditional == 0)
	{
		fragColor = vec4((ambient), 1.0) * 0.6 + mix(fog,(texture(samp, tc) * 0.4), fogFactor);
	}
	//Bump Mapping ON
	else if (shaderConditional == 1)
	{
		N.x = varyingNormal.x + a*sin(b*x);
		N.y = varyingNormal.y + a*sin(b*y);
		N.z = varyingNormal.z + a*sin(b*z);
		N = normalize(N);
		vec3 R = normalize(reflect(-L, N));

		cosPhi = dot(V,R);


		fragColor = mix(fog,(texture(samp, tc)) * material.ambient
				+ light.ambient * material.ambient
				+ light.diffuse * material.diffuse * max(cosTheta, 0.0)
				+ light.specular  * material.specular *pow(max(cosPhi, 0.0), material.shininess), fogFactor);
//		fragColor = globalAmbient * mix(fog, (material.ambient
//		+ light.ambient * material.ambient
//		+ light.diffuse * material.diffuse * max(cosTheta, 0.0)
//		+ light.specular  * material.specular *
//		pow(max(cosPhi, 0.0), material.shininess)), fogFactor);
	}
	//Transparncy mode
	else if (shaderConditional == 2)
	{
		fragColor = globalAmbient * mix(fog, (material.ambient
		+ light.ambient * material.ambient
		+ light.diffuse * material.diffuse * max(dot(L,N),0.0)
		+ light.specular * material.specular
		* pow(max(dot(H,N),0.0),material.shininess*3.0)), fogFactor);

		// the following is added for transparency
		fragColor = vec4(fragColor.xyz, alpha);
	}
	//Nooise
	else if (shaderConditional == 3){
		vec4 t = texture(noiseUniform,originalVertex/3.0 + 0.5);

		// compute ADS contributions (per pixel):
		fragColor = 0.7 * t * (globalAmbient + light.ambient + light.diffuse * max(cosTheta,0.0))
		+ 0.5 * light.specular * pow(max(cosPhi,0.0), material.shininess);
	}

	//We could add code here that says if it's in the bump
	if (notInShadow == 1.0)
	{
		//fragColor += vec4((diffuse + specular), 1.0) * 0.5 + texture(samp, tc) * 0.5;
		fragColor += vec4((diffuse + specular), 1.0), texture(samp, tc) * 0.5;
	}

	fragColor = mix(fog, vec4(fragColor.xyz, alpha), fogFactor);
}