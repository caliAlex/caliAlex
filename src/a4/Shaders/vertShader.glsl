#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 vertNormal;

out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingNormal;
out vec3 varyingHalfVector;
out vec2 tc;
out vec4 shadow_coord;
out vec3 originalVertex;
out vec3 vertEyeSpacePos;

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
{
	//Output vertex position rasterizer for interpolation
	varyingVertPos = (mv_matrix * vec4(vertPos,1.0)).xyz;

	//Get vector from vertex to light and output to rasterizer for interpolation
	varyingLightDir = light.position - varyingVertPos;

	//get vertex normal vector in eye space & output to rasterizer for interp.
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;

	//--FOG-- Height-Mapped Vertex
	vec4 p = vec4(vertPos,1.0) + vec4((vertNormal*((texture2D(height,texCoord).r)/5.0f)),1.0f);
	//--FOG-- Compute vertex position in eye space (without perspective)

	varyingHalfVector = (varyingLightDir-varyingVertPos).xyz;

	// calculate the half vector (L+V)
	//vHalfVec = (varyingLightDir-varyingVertPos).xyz;
	//if rendering a back-face, flip the normal
	if (flipNormal < 0) varyingNormal = -varyingNormal; //This for transparency

	//Calculate shadow coordinates for 2nd pass
	shadow_coord = shadowMVP * vec4(vertPos, 1.0);

	originalVertex = vertPos;	//Added for bump mapping

	tc = texCoord;

	vertEyeSpacePos = (mv_matrix * p).xyz;
	gl_Position = proj_matrix * mv_matrix * (vec4(vertPos,1.0) + p);
	//gl_Position = proj_matrix * mv_matrix * p;

}
